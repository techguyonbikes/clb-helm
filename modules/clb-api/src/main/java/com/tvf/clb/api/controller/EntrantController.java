package com.tvf.clb.api.controller;

import com.tvf.clb.base.entity.Entrant;
import com.tvf.clb.service.service.EntrantService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Flux;


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
    public Flux<Entrant> getEntrantByRaceId(@RequestParam(value = "id", required = true) String id) {
        return entrantService.getEntrantsByRaceId(id);
   }
}
