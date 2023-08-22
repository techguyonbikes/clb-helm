package com.tvf.clb.base.webclient;


import com.tvf.clb.base.utils.AppConstant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import reactor.netty.transport.ProxyProvider;

import java.util.function.Consumer;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Configuration
public class WebClientConfig {

    @Value("${proxy.host}")
    private String proxyHost;

    @Value("${proxy.port}")
    private int proxyPort;

    @Value("${proxy.username}")
    private String proxyUsername;

    @Value("${proxy.password}")
    private String proxyPassword;

    private static final String USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/110.0.0.0 Safari/537.36";

    private final Consumer<? super ProxyProvider.TypeSpec> proxySpecProvider = proxy -> proxy.type(ProxyProvider.Proxy.HTTP).host(proxyHost)
            .port(proxyPort).username(proxyUsername).password(s -> proxyPassword).build();

    private static final Consumer<HttpHeaders> headers = httpHeaders -> {
        httpHeaders.add(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE);
        httpHeaders.add(HttpHeaders.USER_AGENT, USER_AGENT);
    };

    public static WebClient createFromBaseUrl(String baseUrl, HttpClient httpClient) {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .defaultHeaders(headers)
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1600 * 1024 * 1024))
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    public static HttpClient buildHttpClient(Consumer<? super ProxyProvider.TypeSpec> proxyOptions) {
        ConnectionProvider connectionProvider = ConnectionProvider.builder("connectionProvider")
                .maxConnections(20000)
                .build();

        if (proxyOptions == null) {
            return HttpClient.create(connectionProvider);
        } else {
            return HttpClient.create(connectionProvider)
                    .secure()
                    .proxy(proxyOptions);
        }
    }


    @Bean
    public WebClient ladbrokesWebClient() {
        return createFromBaseUrl(AppConstant.LAD_BROKES_BASE_URL, buildHttpClient(null));
    }

    @Bean
    public WebClient zbetsWebClient() {
        return createFromBaseUrl(AppConstant.ZBET_BASE_URL, buildHttpClient(proxySpecProvider));
    }

    @Bean
    public WebClient nedsWebClient() {
        return createFromBaseUrl(AppConstant.NEDS_BASE_URL, buildHttpClient(null));
    }

    @Bean
    public WebClient tabWebClient() {
        return createFromBaseUrl(AppConstant.TAB_BASE_URL, buildHttpClient(null));
    }

    @Bean
    public WebClient pointBetWebClient() {
        return createFromBaseUrl(AppConstant.POINT_BET_BASE_URL, buildHttpClient(null));
    }

    @Bean
    public WebClient sportBetWebClient() {
        return createFromBaseUrl(AppConstant.SPORT_BET_BASE_URL, buildHttpClient(proxySpecProvider));
    }

    @Bean
    public WebClient topSportWebClient() {
        return createFromBaseUrl(AppConstant.TOPSPORT_BASE_URL, buildHttpClient(proxySpecProvider));
    }
    @Bean
    public WebClient playUpWebClient() {
        return createFromBaseUrl(AppConstant.PLAY_UP_BASE_URL, buildHttpClient(proxySpecProvider));
    }

    @Bean
    public WebClient betMWebClient() {
        Consumer<HttpHeaders> headersWithToken = headers.andThen(httpHeaders -> httpHeaders.add(AppConstant.CSRF_HEADER_NAME, AppConstant.CSRF_TOKEN));

        return createFromBaseUrl(AppConstant.BET_M_BASE_URL, buildHttpClient(null))
                    .mutate()
                    .defaultHeaders(headersWithToken)
                    .build();
    }

    @Bean
    public WebClient betFluxWebClient() {
        return createFromBaseUrl(AppConstant.BET_FLUX_BASE_URL, buildHttpClient(null));
    }
    @Bean
    public WebClient colBetWebClientRace() {
        Consumer<HttpHeaders> headersWithToken = headers.andThen(httpHeaders-> {
            httpHeaders.add(HttpHeaders.AUTHORIZATION, AppConstant.AUTHORIZATION);
            httpHeaders.add(HttpHeaders.ACCEPT, AppConstant.ACCEPT);
        });

        return createFromBaseUrl(AppConstant.COLOSSAL_BET_BASE_RACE_URL, buildHttpClient(null))
                .mutate()
                .defaultHeaders(headersWithToken)
                .build();
    }
    @Bean
    public WebClient colBetWebClientMeeting() {
        return createFromBaseUrl(AppConstant.COLOSSAL_BET_BASE_MEETING_URL, buildHttpClient(null));
    }

    @Bean
    public WebClient blueBetWebClient() {
        return createFromBaseUrl(AppConstant.BLUE_BET_BASE_URL, buildHttpClient(proxySpecProvider));
    }
}
