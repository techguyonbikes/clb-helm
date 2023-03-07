package com.tvf.clb.controller;

import com.tvf.clb.base.dto.EntrantDto;
import com.tvf.clb.base.dto.RaceResponseDTO;
import com.tvf.clb.base.entity.AdditionalInfo;
import com.tvf.clb.base.entity.Race;
import com.tvf.clb.base.entity.Results;
import com.tvf.clb.service.repository.AdditionalInfoResponse;
import com.tvf.clb.service.repository.ResultsRepository;
import com.tvf.clb.service.service.CrawlService;
import com.tvf.clb.service.service.RaceService;
import com.tvf.clb.service.service.ResultService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;

@RestController
@RequestMapping("/race")
public class RaceController {
    @Autowired
    private CrawlService crawlService;

    @Autowired
    private RaceService raceService;

    @Autowired
    private ResultService resultService;

    @Autowired
    private AdditionalInfoResponse additionalInfoResponse;

    //today, do we need get 1 by 1 or bulk?
    @GetMapping("/crawl")
    public Flux<EntrantDto> crawlRaceById(@RequestParam(value = "id", required = true) String id) {
        return crawlService.getRaceById(id);
    }

    @GetMapping("")
    public Mono<Race> getRaceById(@RequestParam(value = "id", required = true) String id) {
        return raceService.getRaceById(id);
    }

    @GetMapping("/side-bar-races")
    public Flux<RaceResponseDTO> getListSideBarRaces(@RequestParam(value = "date", required = true) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return raceService.getListSideBarRaces(date);
    }

    @GetMapping("/results")
    public Flux<Results> getResultsByRaceId(@RequestParam(value = "id", required = true) String id) {
        return resultService.getResultsByRaceId(id);
    }

    @GetMapping("/additionalinfo")
    public Flux<AdditionalInfo> getAdditionalInfoByRaceId(@RequestParam(value = "id", required = true) String id) {
        return additionalInfoResponse.findByRaceId(id);
    }
}
