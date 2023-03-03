package com.tvf.clb.service.service;

import com.tvf.clb.base.dto.RaceResponseDTO;
import com.tvf.clb.base.dto.RaceResponseMapper;
import com.tvf.clb.base.entity.Meeting;
import com.tvf.clb.service.repository.RaceRepository;
import com.tvf.clb.base.entity.Race;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;


import java.time.Instant;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Service
@Slf4j
public class RaceService {
    @Autowired
    private RaceRepository raceRepository;

    @Autowired
    private MeetingService meetingService;

    public Mono<Race> getRaceById(String id) {
        return raceRepository.findRaceByRaceId(UUID.fromString(id));
    }

    public Flux<RaceResponseDTO> getListSideBarRaces(LocalDate date) {
        LocalDateTime maxDateTime = date.atTime(23, 59, 59);
        LocalDateTime minDateTime = date.atTime(00, 00, 00);
        Instant endTime = maxDateTime.atOffset(ZoneOffset.UTC).toInstant();
        Instant startTime = minDateTime.atOffset(ZoneOffset.UTC).toInstant();
        Flux<Race> races = raceRepository.findAllByActualStartBetween(startTime, endTime);
        return races.filter(x -> x.getNumber() != null).flatMap(r -> {
            Mono<Meeting> meetingMono = meetingService.getMeetingByMeetingId(UUID.fromString(r.getMeetingId()));
            return meetingMono.map(meeting -> RaceResponseMapper.toRaceResponseDTO(meeting, r));
        });
    @Autowired
    private MeetingService meetingService;

    public Mono<Race> getRaceById(String id) {
        return raceRepository.findRaceByRaceId(UUID.fromString(id));
    }

    public Flux<RaceResponseDTO> getListSideBarRaces(LocalDate date) {
        LocalDateTime maxDateTime = date.atTime(23, 59, 59);
        LocalDateTime minDateTime = date.atTime(00, 00, 00);
        Instant endTime = maxDateTime.atOffset(ZoneOffset.UTC).toInstant();
        Instant startTime = minDateTime.atOffset(ZoneOffset.UTC).toInstant();
        Flux<Race> races = raceRepository.findAllByActualStartBetween(startTime, endTime);
//        Flux<Race> races = raceRepository.findAll();
        return races.filter(x -> x.getNumber() != null).flatMap(r -> {
            Mono<Meeting> meetingMono = meetingService.getMeetingByMeetingId(UUID.fromString(r.getMeetingId()));
            return meetingMono.map(meeting -> RaceResponseMapper.toRaceResponseDTO(meeting, r));
        });
    }
}
