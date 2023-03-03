package com.clb.ws;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

@SpringBootApplication
@ComponentScan(basePackages = "com.tvf.clb")
@EnableR2dbcRepositories(basePackages = "com.tvf.clb")
public class CloudBetWebSocket {
    public static void main(String[] args) {
        SpringApplication.run(CloudBetWebSocket.class, args);
    }
}
