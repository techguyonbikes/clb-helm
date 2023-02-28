package com.tvf.clb.api.controller;

import com.tvf.clb.api.service.CrawlService;
import com.tvf.clb.base.dto.EntrantDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/race")
public class RaceController {
    @Autowired
    private CrawlService crawlService;

    @GetMapping("")
    @Async("crawlLadbrokesBet")
    public Flux<EntrantDto> getRaceById(@RequestParam(value = "id", required = true) String id) {
        return Mono.just(crawlService.getRaceById(id)).flatMapMany(Flux::fromIterable).log();
    }
}
