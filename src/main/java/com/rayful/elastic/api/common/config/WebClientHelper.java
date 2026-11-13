package com.rayful.elastic.api.common.config;

import org.springframework.web.reactive.function.client.WebClient;

public class WebClientHelper {
    private final WebClient webHttpClient;
    private final WebClient webHttpsClient;

    public WebClientHelper(WebClient webHttpClient, WebClient webHttpsClient) {
        this.webHttpClient =  webHttpClient;
        this.webHttpsClient =  webHttpsClient;
    }

    public WebClient get(boolean useSsl) {
        if (useSsl) {
            return this.webHttpsClient;
        } else {
            return this.webHttpClient;
        }
    }

    public WebClient get(String url) {
        return get(url.startsWith("https"));
    }


}
