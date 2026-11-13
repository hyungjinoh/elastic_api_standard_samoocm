package com.rayful.elastic.api.common.config;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;

//import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.rayful.elastic.api.standard.controller.CommonInterceptor;

import io.netty.channel.ChannelOption;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.client.HttpClientSecurityUtils;
import reactor.netty.resources.ConnectionProvider;
import reactor.netty.tcp.SslProvider;

import java.util.function.Consumer;

import io.netty.handler.ssl.SslHandler;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;

import javax.net.ssl.*;

@Slf4j
@Configuration
public class WebConfig implements WebMvcConfigurer {
	
	@Value("${elasticsearch.host}")
    private String baseUrl;
	  
	@Value("${webclient.max-connections}")
    private int maxConnections;
	 
	@Value("${webclient.max-idle-time}")
	private int maxIdleTime;
	
	@Value("${webclient.max-life-time}")
    private int maxLifeTime;
	
	@Value("${webclient.pending-acquire-timeout}")
    private int pendingAcquireTimeout;
	
	@Value("${webclient.pending-acquire-maxCount}")
    private int pendingAcquireMaxCount;
	
	@Value("${webclient.evict-in-background}")
    private int evictInBackground;
	
	@Value("${webclient.metrics}")
    private boolean isMetrics;
	
	@Value("${webclient.connection-timeout}")
    private int connectionTimeout;
	
	@Value("${webclient.response-timeout}")
    private int responseTimeout;
	
	@Value("${webclient.read-timeout}")
    private int readTimeout;
	
	@Value("${webclient.write-timeout}")
    private int writeTimeout;
	
    @Value("${webclient.max-in-memory-size}")
    private int maxInMemorySize;
    
    @Value("${webclient.ssl.key-store-path:}")
    private String keyStorePath;

    @Value("${webclient.ssl.key-store-pwd:}")
    private String keyStorePassword;

    // 인증서 검증을 우회해야 하는 경우(자체서명 인증서 등) 명시적으로 켜야만 동작 (기본값 false)
    @Value("${webclient.ssl.trust-self-signed:false}")
    private boolean trustSelfSigned;

    @Value("${spring.profiles.active:local}")
    private String activeProfile;
    
    private final CommonInterceptor commonInterceptor;

    // 생성자 주입
    public WebConfig(CommonInterceptor commonInterceptor) {
        this.commonInterceptor = commonInterceptor;
    }

	@Bean
	public WebClientHelper getWebClient() throws GeneralSecurityException, IOException {
		
        ConnectionProvider connectionProvider = ConnectionProvider.builder("custom-provider")
                .maxConnections(this.maxConnections)
                .maxIdleTime(Duration.ofSeconds(this.maxIdleTime))
                .maxLifeTime(Duration.ofSeconds(this.maxLifeTime))
                .pendingAcquireTimeout(Duration.ofMillis(this.pendingAcquireTimeout))
                .pendingAcquireMaxCount(this.pendingAcquireMaxCount)
                .evictInBackground(Duration.ofSeconds(this.evictInBackground))
                .lifo()
                .metrics(this.isMetrics)
                .build();
		
		HttpClient httpClient = HttpClient.create(connectionProvider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, this.connectionTimeout)
                .responseTimeout(Duration.ofMillis(this.responseTimeout))
                .doOnConnected(conn ->
                        conn.addHandlerLast(new ReadTimeoutHandler(this.readTimeout, TimeUnit.MILLISECONDS))
                                .addHandlerLast(new WriteTimeoutHandler(this.writeTimeout, TimeUnit.MILLISECONDS)));
		
		// ssl 추가
		HttpClient httpsClient = httpClient.secure(this.getSslContext());
		
				
		WebClient webHttpsClient = WebClient.builder()
				.baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpsClient))
                // Spring Framework 7: WebClient.Builder.exchangeStrategies(...) 제거됨 -> codecs()로 직접 설정
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(this.maxInMemorySize * 1024 * 1024))
                .filter(ExchangeFilterFunction.ofRequestProcessor(
                        clientRequest -> {
                            log.debug("Request: {} {}", clientRequest.method(), clientRequest.url());
                            clientRequest.headers()
                                    .forEach((name, values) -> values.forEach(value -> log.debug("Header-> {} : {}", name, value)));
                            return Mono.just(clientRequest);
                        }
                ))
                .filter(ExchangeFilterFunction.ofResponseProcessor(
                        clientResponse -> {
                            clientResponse.headers()
                                    .asHttpHeaders()
                                    .forEach((name, values) ->
                                            values.forEach(value -> log.debug("Header-> {} : {}", name, value)));
                            return Mono.just(clientResponse);
                        }
                ))
                .build();

        WebClient webHttpClient = webHttpsClient.mutate().clientConnector(new ReactorClientHttpConnector(httpClient)).build();

        return new WebClientHelper(webHttpClient, webHttpsClient);
	}
	
	/**
	 * ES 접속용 SSL 설정 생성
	 *
	 * [보안 수정 이력]
	 * - (수정 전) keyStorePath가 비어있으면 InsecureTrustManagerFactory로 모든 인증서를 무조건 신뢰 +
	 *             호스트명 검증까지 비활성화 (MITM 공격에 완전히 노출되는 상태였음)
	 * - (수정 전) keyStorePath가 있는 정상 케이스에서도 호스트명 검증을 실수로 꺼두고 있었음
	 *
	 * [수정 후 3단계 우선순위]
	 * 1) keyStorePath 설정됨            → 해당 키스토어/트러스트스토어로 검증 + 호스트명 검증 ON (정상 운영)
	 * 2) keyStorePath 없고 trust-self-signed=true → 인증서 검증은 우회하되, 반드시 명시적으로 켜야 하고
	 *                                     기동 시점에 큰 경고 로그를 남김 (자체서명 인증서를 쓰는 폐쇄망 등 예외 상황 전용)
	 * 3) 둘 다 아니면(기본값)            → JVM 기본 트러스트스토어(공인 CA)로 검증 + 호스트명 검증 ON (가장 안전한 기본값)
	 */
	@SuppressWarnings("unchecked")
	private SslProvider getSslContext() throws IOException, GeneralSecurityException {
	    SslContext sslContext;
	    final Consumer<SslHandler> handlerConfigurator;

	    if (this.keyStorePath != null && !this.keyStorePath.isEmpty()) {

	       log.info("ssl.key-store-path: {}", this.keyStorePath);

	       KeyStore keyStore = KeyStore.getInstance("JKS");
	       try (InputStream keyStoreStream = getClass().getClassLoader().getResourceAsStream(this.keyStorePath)) {
	          if (keyStoreStream == null) {
	             throw new FileNotFoundException("인증서 파일을 찾을 수 없습니다: " + this.keyStorePath);
	          }
	          keyStore.load(keyStoreStream, this.keyStorePassword.toCharArray());
	       }

	       KeyManagerFactory keyFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
	       keyFactory.init(keyStore, this.keyStorePassword.toCharArray());

	       TrustManagerFactory trustFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
	       trustFactory.init(keyStore);

	       sslContext = SslContextBuilder.forClient()
	             .keyManager(keyFactory)
	             .trustManager(trustFactory)
	             .build();

	       // 정식 키스토어를 쓰는 경우 호스트명 검증은 반드시 켜둔 채로 둔다 (기본 동작 그대로 사용)
	       handlerConfigurator = (Consumer<SslHandler>) HttpClientSecurityUtils.HOSTNAME_VERIFICATION_CONFIGURER;

	    } else if (this.trustSelfSigned) {

	       // [고객사 변경] 원래는 은행 폐쇄망 전제라 dev/prod 에서 이 옵션 자체를 막아뒀었다(실수로 배포되는
	       // 사고 방지 목적). 이제 일반 회사 폐쇄망이 아닌 환경에서 운영하기로 하면서, dev/prod 에서도
	       // trust-self-signed=true 를 허용하도록 그 차단 로직을 제거했다(local 과 동일하게 동작).
	       // 참고: 그래도 자체서명 인증서 우회이므로 운영 반영 전 사내 보안 정책과 다시 한번 확인할 것.

	       log.warn("=================================================================");
	       log.warn(" webclient.ssl.trust-self-signed=true 로 인증서 검증을 우회합니다.");
	       log.warn(" 이 설정은 자체서명 인증서를 쓰는 폐쇄망 등 예외적인 상황에서만 사용해야 하며,");
	       log.warn(" 외부에 노출되는 환경에서는 절대 사용하면 안 됩니다.");
	       log.warn("=================================================================");

	       sslContext = SslContextBuilder.forClient()
	             .trustManager(InsecureTrustManagerFactory.INSTANCE)
	             .build();

	       handlerConfigurator = sslHandler -> {
	          SSLParameters sslParameters = sslHandler.engine().getSSLParameters();
	          sslParameters.setEndpointIdentificationAlgorithm(null);
	          sslHandler.engine().setSSLParameters(sslParameters);
	       };

	    } else {

	       log.info("ssl.key-store-path is not set. Using JVM default trust store (public CA).");

	       // 커스텀 트러스트매니저를 지정하지 않으면 JVM 기본 트러스트스토어(공인 CA 목록)로 검증한다.
	       sslContext = SslContextBuilder.forClient().build();
	       handlerConfigurator = (Consumer<SslHandler>) HttpClientSecurityUtils.HOSTNAME_VERIFICATION_CONFIGURER;
	    }

	    return SslProvider.builder()
	          .sslContext(sslContext)
	          .handlerConfigurator(handlerConfigurator)
	          .build();
	}

	@Override
    public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(commonInterceptor)
		        .excludePathPatterns(
						"/api/license",
						"/swagger-ui.html",
						"/swagger-ui/**",
						"/v3/api-docs",
						"/v3/api-docs/**",
						"/swagger-resources/**",
						"/webjars/**"
				);
    }
}
