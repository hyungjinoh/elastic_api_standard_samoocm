package com.rayful.elastic.api.loader.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Elasticsearch 접속 설정. application.yml 의 {@code elasticsearch.*} 바인딩.
 * (Lombok 미사용 - 명시적 getter/setter)
 */
@ConfigurationProperties(prefix = "elasticsearch")
public class ElasticsearchProperties {

    /** 예: https://elasticsearch:9200 (인증서 SAN 과 일치하는 호스트명 사용) */
    private String host = "https://localhost:9200";

    /** basic 인증 사용자. 폐쇄망에서는 apiKey 방식 권장 */
    private String username;
    private String password;

    /** API Key 방식(id:secret Base64 또는 encoded). username/password 보다 우선 */
    private String apiKey;

    /** JKS 트러스트스토어 경로. 지정 시 self-signed 인증서를 여기서 검증 */
    private String truststorePath;
    private String truststorePassword;

    /**
     * self-signed 인증서 전체 신뢰(검증 우회). 개발/테스트 전용.
     * prod/stg 프로파일에서는 기동 자체를 차단(fail-fast)한다.
     */
    private boolean trustSelfSigned = false;

    private int connectTimeoutMs = 5000;
    private int socketTimeoutMs = 60000;

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getApiKey() { return apiKey; }
    public void setApiKey(String apiKey) { this.apiKey = apiKey; }

    public String getTruststorePath() { return truststorePath; }
    public void setTruststorePath(String truststorePath) { this.truststorePath = truststorePath; }

    public String getTruststorePassword() { return truststorePassword; }
    public void setTruststorePassword(String truststorePassword) { this.truststorePassword = truststorePassword; }

    public boolean isTrustSelfSigned() { return trustSelfSigned; }
    public void setTrustSelfSigned(boolean trustSelfSigned) { this.trustSelfSigned = trustSelfSigned; }

    public int getConnectTimeoutMs() { return connectTimeoutMs; }
    public void setConnectTimeoutMs(int connectTimeoutMs) { this.connectTimeoutMs = connectTimeoutMs; }

    public int getSocketTimeoutMs() { return socketTimeoutMs; }
    public void setSocketTimeoutMs(int socketTimeoutMs) { this.socketTimeoutMs = socketTimeoutMs; }
}
