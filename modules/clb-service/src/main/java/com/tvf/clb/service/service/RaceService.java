package com.tvf.clb.service.service;

import com.tvf.clb.base.dto.RaceResponseDTO;
import com.tvf.clb.base.dto.RaceResponseMapper;
import com.tvf.clb.base.entity.Meeting;
import com.tvf.clb.base.entity.Race;
import com.tvf.clb.service.repository.MeetingRepository;
import com.tvf.clb.service.repository.RaceRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.*;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class RaceService {
    @Autowired
    private RaceRepository raceRepository;

    @Autowired
    private MeetingRepository meetingRepository;
    @Autowired
    private MeetingService meetingService;

    private static final String SIDE_NAME_PREFIX = "R";

    public Mono<Race> getRaceById(String raceId) {
        return raceRepository.findRaceByRaceId(raceId);
    }

    public Flux<RaceResponseDTO> getListSideBarRaces(LocalDate date) {
        LocalDateTime maxDateTime = date.atTime(LocalTime.MAX);
        LocalDateTime minDateTime = date.atTime(LocalTime.MIN);
        Instant endTime = maxDateTime.atOffset(ZoneOffset.UTC).toInstant();
        Instant startTime = minDateTime.atOffset(ZoneOffset.UTC).toInstant();
        Flux<Race> races = raceRepository.findAllByActualStartBetween(startTime, endTime)
                .sort(Comparator.comparing(Race::getActualStart));
        return races.filter(x -> x.getNumber() != null).flatMap(r -> {
            Mono<Meeting> meetingMono = meetingService.getMeetingByMeetingId(r.getMeetingId());
            return meetingMono.map(meeting -> RaceResponseMapper.toRaceResponseDTO(meeting, r));
        });
    }

    public Flux<RaceResponseDTO> getFilter(LocalDate date, List<String> meetingIds, List<RaceType> raceTypes) {
        List<String> raceType = raceTypes.stream()
                .map(RaceType::toString)
                .collect(Collectors.toList());
        Flux<RaceResponseDTO> raceResponse;
        LocalDateTime dateTime = date.atTime(LocalTime.MIN);
        Instant startDate = dateTime.atOffset(ZoneOffset.UTC).toInstant();
        if (CollectionUtils.isEmpty(meetingIds)) {
            raceResponse = meetingRepository.findByRaceTypes(raceType, startDate)
                    .map(r -> {
                        r.setSideName(SIDE_NAME_PREFIX + r.getNumber() + " " + r.getMeetingName());
                        return r;
                    });

        } else {
            raceResponse = meetingRepository.findByRaceTypeAndMeetingId(raceType,meetingIds,startDate)
                    .map(r -> {
                        r.setSideName(SIDE_NAME_PREFIX + r.getNumber() + " " + r.getMeetingName());
                        return r;
                    });
        }
        return raceResponse;
    }

}
