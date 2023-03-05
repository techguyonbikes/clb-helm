package com.tvf.clb.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.flywaydb.core.Flyway;

@Configuration
public class FlywayConfiguration {

    private final Environment env;
    private final static String DEFAULT_SCHEMA = "clb_db";

    public FlywayConfiguration(Environment env) {
        this.env = env;
    }

    @Bean(initMethod = "migrate")
    public Flyway flyway() {
        return new Flyway(Flyway.configure()
                .schemas(DEFAULT_SCHEMA)
                .dataSource(
                        env.getRequiredProperty("spring.flyway.url"),
                        env.getRequiredProperty("spring.flyway.user"),
                        env.getRequiredProperty("spring.flyway.password"))
                .outOfOrder(true));
    }
}
