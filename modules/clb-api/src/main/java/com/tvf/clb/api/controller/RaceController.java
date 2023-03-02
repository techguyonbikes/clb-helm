package com.tvf.clb.api.controller;

import com.tvf.clb.base.dto.RaceResponseDTO;
import com.tvf.clb.service.service.CrawlService;
import com.tvf.clb.service.service.RaceService;
import com.tvf.clb.base.dto.EntrantDto;
import com.tvf.clb.base.dto.RaceDto;
import com.tvf.clb.base.entity.Race;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/race")
public class RaceController {
    @Autowired
    private CrawlService crawlService;

    @Autowired
    private RaceService raceService;

    //today, do we need get 1 by 1 or bulk?
    @GetMapping("/crawl")
    public Flux<EntrantDto> crawlRaceById(@RequestParam(value = "id", required = true) String id) {
        return crawlService.getRaceById(id);
    }

    @GetMapping("/all")
    public Flux<Race> getAllRace() {
        return raceService.getAllRace();
    }

//    @GetMapping("")
//    public Mono<Race> getRaceById(@RequestParam(value = "id", required = true) String id) {
//        return raceService.getRaceById(id);
//    }

    @GetMapping("/side-bar-races")
    public Flux<RaceResponseDTO> getListSideBarRaces() {
        return raceService.getListSideBarRaces();
    }
}
