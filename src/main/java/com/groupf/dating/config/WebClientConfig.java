package com.groupf.dating.config;

import com.groupf.dating.common.ApiConstants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class WebClientConfig {

    @Value("${claude.api.key}")
    private String claudeApiKey;

    @Value("${claude.api.url}")
    private String claudeApiUrl;

    @Bean
    public RestClient claudeRestClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(ApiConstants.CONNECTION_TIMEOUT_SECONDS));
        factory.setReadTimeout(Duration.ofSeconds(ApiConstants.API_TIMEOUT_SECONDS));

        return RestClient.builder()
                .baseUrl(claudeApiUrl)
                .requestFactory(factory)
                .defaultHeader(ApiConstants.HEADER_API_KEY, claudeApiKey)
                .defaultHeader(ApiConstants.HEADER_API_VERSION, ApiConstants.CLAUDE_API_VERSION)
                .defaultHeader(ApiConstants.HEADER_CONTENT_TYPE, ApiConstants.MEDIA_TYPE_JSON)
                .build();
    }
}
