package com.tvf.clb.api.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/ping")
public class HealthCheckController {

    @GetMapping
    public Mono<String> ping() {
        return Mono.just("pong");
    }
}
