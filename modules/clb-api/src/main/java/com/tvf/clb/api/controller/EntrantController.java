package com.tvf.clb.api.controller;

import com.tvf.clb.base.dto.EntrantDto;
import com.tvf.clb.base.dto.MeetingDto;
import com.tvf.clb.base.dto.RaceResponseDTO;
import com.tvf.clb.base.entity.Entrant;
import com.tvf.clb.base.entity.Race;
import com.tvf.clb.service.service.EntrantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/entrant")
public class EntrantController {

    @Autowired
    EntrantService entrantService;

    @GetMapping("/all")
    public Flux<Entrant> getAllEntrant() {
        return entrantService.getAllEntrant();
    }

    @GetMapping("")
    public Flux<Entrant> getRaceById(@RequestParam(value = "id", required = true) String id) {
        return entrantService.getEntrantsByRaceId(id);
   }
}
