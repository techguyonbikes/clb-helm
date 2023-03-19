package com.tvf.clb.controller;

import com.tvf.clb.base.dto.RaceResponseDTO;
import com.tvf.clb.base.entity.Race;
import com.tvf.clb.service.service.RaceService;
import com.tvf.clb.service.service.RaceType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/race")
public class RaceController {

    @Autowired
    private RaceService raceService;


    @GetMapping("")
    public Mono<Race> getRaceById(@RequestParam(value = "id", required = true) Long id) {
        return raceService.getRaceById(id);
    }

    @GetMapping("/side-bar-races")
    public Flux<RaceResponseDTO> searchRaces(@RequestParam(value = "date", required = true) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                                       @RequestParam(value = "meetingIds", required = false) List<Long> meetingIds,
                                                       @RequestParam(value = "raceTypes", required = false, defaultValue = "Horse,Greyhound,Harness") List<RaceType> raceTypes) {
        return raceService.searchRaces(date, meetingIds, raceTypes);
    }
}
