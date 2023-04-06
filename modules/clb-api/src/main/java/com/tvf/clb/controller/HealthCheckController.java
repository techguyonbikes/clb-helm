package com.tvf.clb.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.stream.IntStream;

@RestController
@RequestMapping("/ping")
@Slf4j
public class HealthCheckController {

    @GetMapping
    public Mono<String> ping() {
        Flux.fromArray(IntStream.range(0, 1000).boxed().toArray())
                .parallel()
                .runOn(Schedulers.parallel())
                .doOnNext(i -> {
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    log.info(i.toString());
                })
                .subscribe();
        return Mono.just("pong");
    }
}
