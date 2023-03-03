package com.tvf.clb.service.service;

import com.tvf.clb.base.entity.Entrant;
import com.tvf.clb.base.entity.Race;
import com.tvf.clb.service.repository.EntrantRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
@Slf4j
public class EntrantService {
    @Autowired
    EntrantRepository entrantRepository;

    public Flux<Entrant> getAllEntrant() {
        return entrantRepository.findAll();

    }


}
