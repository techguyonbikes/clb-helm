package com.tvf.clb.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.*;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.contexts.SecurityContext;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2WebFlux;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableSwagger2WebFlux
public class SwaggerConfig {

    public static final String AUTHORIZATION_HEADER = "Authorization";
    private static final Logger LOGGER = LoggerFactory.getLogger(SwaggerConfig.class);

    @Bean
    public Docket swaggerSpringfoxDocket() {
        LOGGER.info("Starting Swagger");
        Contact contact = new Contact(
                "Cloudbet",
                "",
                "techvify@email.com");

        ApiInfo apiInfo = new ApiInfo(
                "Backend API",
                "",
                "1.0.0",
                "https://techvify.com/",
                contact,
                "MIT",
                "https://techvify.com/",
                new ArrayList<>());

        List<SecurityContext> securityContexts = new ArrayList<>();
        securityContexts.add(securityContext());
        List<SecurityScheme> apiKeys = new ArrayList<>();
        apiKeys.add(apiKey());

        return new Docket(DocumentationType.SWAGGER_2)
                .apiInfo(apiInfo)
                .securityContexts(Arrays.asList(securityContext()))
                .securitySchemes(Arrays.asList(apiKey()))
                .select()
                .apis(RequestHandlerSelectors.any())
                .paths(PathSelectors.any())
                .build();
    }


    private ApiKey apiKey() {
        return new ApiKey("JWT", AUTHORIZATION_HEADER, "header");
    }

    private SecurityContext securityContext() {
        return SecurityContext.builder().securityReferences(defaultAuth()).build();
    }

    List<SecurityReference> defaultAuth() {
        AuthorizationScope authorizationScope
                = new AuthorizationScope("global", "accessEverything");
        AuthorizationScope[] authorizationScopes = new AuthorizationScope[1];
        authorizationScopes[0] = authorizationScope;
        return Arrays.asList(new SecurityReference("JWT", authorizationScopes));
    }
}
