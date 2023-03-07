package com.tvf.clb.service.service;

import com.tvf.clb.base.entity.Results;
import com.tvf.clb.service.repository.ResultsRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
@Slf4j
public class ResultService {
    @Autowired
    ResultsRepository resultsRepository;

    public Flux<Results> getResultsByRaceId(String id){
        return resultsRepository.findByRaceId(id);
    }


}
