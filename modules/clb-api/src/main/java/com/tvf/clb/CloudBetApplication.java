package com.tvf.clb;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
//@EnableScheduling
@EnableR2dbcRepositories(basePackages = "com.tvf.clb")
public class CloudBetApplication {

    public static void main(String[] args) {
        SpringApplication.run(CloudBetApplication.class, args);
    }

}