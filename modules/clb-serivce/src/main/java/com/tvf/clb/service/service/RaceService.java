package com.tvf.clb.service.service;

import com.tvf.clb.base.dto.RaceResponseDTO;
import com.tvf.clb.base.dto.RaceResponseMapper;
import com.tvf.clb.base.entity.Meeting;
import com.tvf.clb.service.repository.RaceRepository;
import com.tvf.clb.base.entity.Race;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
public class RaceService {
    @Autowired
    private RaceRepository raceRepository;

    @Autowired
    private MeetingService meetingService;

    private RaceResponseMapper raceResponseMapper;

    public Flux<Race> getAllRace() {
        return raceRepository.findAll();
    }

    public Mono<Race> getRaceById(String id) {
        return raceRepository.findRaceByRaceId(UUID.fromString(id));
    }

    @Async()
    public Flux<RaceResponseDTO> getListSideBarRaces() {
        List<Race> races = getAllRace().collectList();
        List<RaceResponseDTO> responseDTOS = races.stream().map(race -> {
            Mono<Meeting> meeting = meetingService.getMeetingByMeetingId(UUID.fromString(race.getMeetingId()));
            meeting.subscribe();
            return raceResponseMapper.toRaceResponseDTO(meeting.block(), race);
        });
        return responseDTOS;
    }
}
