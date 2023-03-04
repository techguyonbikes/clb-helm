package com.tvf.clb.config;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.reactive.config.CorsRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;

import java.util.ArrayList;
import java.util.List;

@Configuration
@Slf4j
public class WebCorsConfig implements WebFluxConfigurer {

    @Autowired
    private CorsConfigProperties corsConfigProperties;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        if (log.isDebugEnabled()) {
            log.debug("[addCorsMappings] allowOrigins : {}", corsConfigProperties.getAllowOrigins());
        }
        registry.addMapping("/**")
                .allowedOrigins(corsConfigProperties.getAllowOrigins().toArray(new String[0]))
                .allowedMethods(RequestMethod.POST.name(), RequestMethod.GET.name(), RequestMethod.PUT.name(), RequestMethod.DELETE.name(), RequestMethod.OPTIONS.name())
                .maxAge(corsConfigProperties.getMaxAge())
                .allowCredentials(corsConfigProperties.getAllowCredentials())
                .allowedHeaders("Authorization", "Cache-Control", "Content-Type", "Referer", "X-XSRF-TOKEN", "Accept", "Access-Control-Allow-Origin");
    }
}

@Component
@Getter
@Setter
@ConfigurationProperties(prefix = "app.cors")
class CorsConfigProperties {
    List<String> allowOrigins = new ArrayList<>();
    Integer maxAge = 600;
    Boolean allowCredentials = true;
}