package com.tvf.clb.base.webclient;


import com.tvf.clb.base.utils.AppConstant;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.util.function.Consumer;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Configuration
public class WebClientConfig {
    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/110.0.0.0 Safari/537.36";

    private static final Consumer<HttpHeaders> headers = httpHeaders -> {
        httpHeaders.add(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE);
        httpHeaders.add(HttpHeaders.USER_AGENT, USER_AGENT);
    };

    public static WebClient createFromBaseUrl(String baseUrl) {
        HttpClient httpClient = HttpClient.create(ConnectionProvider.builder("connectionProvider").maxConnections(20000).build());
        return WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeaders(headers)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1600 * 1024 * 1024))
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    @Bean
    public WebClient ladbrokesWebClient() {
        return createFromBaseUrl(AppConstant.LAD_BROKES_BASE_URL);
    }

    @Bean
    public WebClient zbetsWebClient() {
        return createFromBaseUrl(AppConstant.ZBET_BASE_URL);
    }

    @Bean
    public WebClient nedsWebClient() {
        return createFromBaseUrl(AppConstant.NEDS_BASE_URL);
    }

    @Bean
    public WebClient tabWebClient() {
        return createFromBaseUrl(AppConstant.TAB_BASE_URL);
    }

    @Bean
    public WebClient pointBetWebClient() {
        return createFromBaseUrl(AppConstant.POINT_BET_BASE_URL);
    }

    @Bean
    public WebClient sportBetWebClient() {
        return createFromBaseUrl(AppConstant.SPORT_BET_BASE_URL);
    }

}
