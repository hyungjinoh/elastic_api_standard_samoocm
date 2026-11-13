package com.rayful.elastic.api.loader.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.message.BasicHeader;
import org.elasticsearch.client.RestClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.Arrays;

import org.apache.http.Header;
import org.apache.http.ssl.SSLContexts;

/**
 * Elasticsearch 8.x 공식 Java 클라이언트 구성.
 *
 * SSL 전략(3분기):
 *  1) truststorePath 지정  → JKS 트러스트스토어로 self-signed 인증서 검증
 *  2) trustSelfSigned=true → 검증 우회(개발 전용). prod/stg 프로파일에서는 기동 차단
 *  3) 그 외                → JVM 기본 트러스트스토어 사용
 */
@Configuration
@EnableConfigurationProperties(ElasticsearchProperties.class)
public class ElasticsearchConfig {

    private static final Logger log = LoggerFactory.getLogger(ElasticsearchConfig.class);

    private final ElasticsearchProperties props;
    private final Environment env;

    public ElasticsearchConfig(ElasticsearchProperties props, Environment env) {
        this.props = props;
        this.env = env;
    }

    @Bean(destroyMethod = "close")
    public RestClient restClient() throws Exception {
        URI uri = URI.create(props.getHost());
        HttpHost httpHost = new HttpHost(uri.getHost(), uri.getPort(), uri.getScheme());

        final SSLContext sslContext = buildSslContext(uri.getScheme());

        RestClientBuilderHolder holder = new RestClientBuilderHolder();
        RestClient client = RestClient.builder(httpHost)
                .setHttpClientConfigCallback(cb -> {
                    if (sslContext != null) {
                        cb.setSSLContext(sslContext);
                    }
                    // 인증: API Key 우선, 없으면 basic
                    if (StringUtils.hasText(props.getApiKey())) {
                        cb.setDefaultHeaders(java.util.List.of(
                                (Header) new BasicHeader("Authorization", "ApiKey " + props.getApiKey())));
                    } else if (StringUtils.hasText(props.getUsername())) {
                        CredentialsProvider cp = new BasicCredentialsProvider();
                        cp.setCredentials(AuthScope.ANY,
                                new UsernamePasswordCredentials(props.getUsername(), props.getPassword()));
                        cb.setDefaultCredentialsProvider(cp);
                    }
                    return cb;
                })
                .setRequestConfigCallback(rc -> rc
                        .setConnectTimeout(props.getConnectTimeoutMs())
                        .setSocketTimeout(props.getSocketTimeoutMs()))
                .build();
        holder.client = client;
        return client;
    }

    @Bean
    public ElasticsearchClient elasticsearchClient(RestClient restClient) {
        // [병합 시 수정] 원래는 Spring 이 관리하는 ObjectMapper 빈을 주입받아 썼는데, 이 호스트 앱은
        // Spring Boot 4 라 기본 JSON 빈이 Jackson 3(tools.jackson.databind.ObjectMapper) 이다.
        // JacksonJsonpMapper(elasticsearch-java)는 Jackson 2(com.fasterxml.jackson.databind.ObjectMapper)
        // 를 요구해서, 타입이 안 맞아 "No qualifying bean" 으로 기동이 실패했다.
        // → Spring 빈을 주입받지 않고, 이 ES 클라이언트 전용으로 Jackson 2 ObjectMapper 를 직접 생성해서 쓴다
        //   (앱 전체의 JSON 직렬화 설정과는 무관하게, ES 클라이언트 내부에서만 쓰이는 인스턴스).
        ObjectMapper objectMapper = new ObjectMapper();
        ElasticsearchTransport transport =
                new RestClientTransport(restClient, new JacksonJsonpMapper(objectMapper));
        return new ElasticsearchClient(transport);
    }

    // ---------------------------------------------------------------------

    private SSLContext buildSslContext(String scheme) throws Exception {
        if (!"https".equalsIgnoreCase(scheme)) {
            return null; // http 이면 SSL 컨텍스트 불필요
        }

        // 1) JKS 트러스트스토어
        if (StringUtils.hasText(props.getTruststorePath())) {
            Path p = Path.of(props.getTruststorePath());
            if (!Files.exists(p)) {
                throw new IllegalStateException("트러스트스토어 파일을 찾을 수 없습니다: " + p);
            }
            KeyStore ks = KeyStore.getInstance("JKS");
            char[] pw = props.getTruststorePassword() == null
                    ? new char[0] : props.getTruststorePassword().toCharArray();
            try (InputStream in = Files.newInputStream(p)) {
                ks.load(in, pw);
            }
            log.info("[ES-SSL] JKS 트러스트스토어로 인증서 검증: {}", p);
            return SSLContexts.custom().loadTrustMaterial(ks, null).build();
        }

        // 2) self-signed 전체 신뢰 (개발 전용) — prod/stg 차단
        if (props.isTrustSelfSigned()) {
            String[] active = env.getActiveProfiles();
            boolean blocked = Arrays.stream(active)
                    .anyMatch(pf -> pf.equalsIgnoreCase("prod") || pf.equalsIgnoreCase("stg"));
            if (blocked) {
                throw new IllegalStateException(
                        "trust-self-signed 는 prod/stg 프로파일에서 사용할 수 없습니다. JKS 트러스트스토어를 설정하세요.");
            }
            log.warn("[ES-SSL] self-signed 인증서 검증을 우회합니다. 개발 환경에서만 사용하세요.");
            return SSLContexts.custom()
                    .loadTrustMaterial(null, (chain, authType) -> true)
                    .build();
        }

        // 3) JVM 기본 트러스트스토어
        log.info("[ES-SSL] JVM 기본 트러스트스토어 사용");
        return SSLContext.getDefault();
    }

    /** RestClient close 를 destroyMethod 로 걸기 위한 홀더(가독성용). */
    private static final class RestClientBuilderHolder {
        RestClient client;
    }

    // trust-self-signed 우회용 X509 TrustManager (참고용, SSLContexts 사용으로 미참조)
    @SuppressWarnings("unused")
    private static TrustManager[] trustAll() {
        return new TrustManager[]{new X509TrustManager() {
            public void checkClientTrusted(X509Certificate[] c, String a) { }
            public void checkServerTrusted(X509Certificate[] c, String a) { }
            public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
        }};
    }
}
