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

    public Mono<Race> getRaceById(Long raceId) {
        return raceRepository.findById(raceId);
    }

    public Flux<RaceResponseDTO> searchRaces(LocalDate date, List<Long> meetingIds, List<RaceType> raceTypes) {
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
            raceResponse = meetingRepository.findByRaceTypeAndMeetingId(raceType, meetingIds, startDate)
                    .map(r -> {
                        r.setSideName(SIDE_NAME_PREFIX + r.getNumber() + " " + r.getMeetingName());
                        return r;
                    });
        }
        return raceResponse.sort(Comparator.comparing(RaceResponseDTO::getDate));
    }

    public Flux<Race> findAllRacesInSameMeetingByRaceId(Long raceId) {
        Mono<Race> raceMono = raceRepository.findById(raceId);
        return raceMono.flatMapMany(race -> raceRepository.findAllByMeetingId(race.getMeetingId()));
    }

    public Flux<RaceResponseDTO> getListRaceDefault(LocalDate date) {
        LocalDateTime maxDateTime = date.plusDays(3).atTime(23, 59, 59);
        LocalDateTime minDateTime = date.plusDays(-3).atTime(00, 00, 00);
        Instant endTime = maxDateTime.atOffset(ZoneOffset.UTC).toInstant();
        Instant startTime = minDateTime.atOffset(ZoneOffset.UTC).toInstant();
        Flux<RaceResponseDTO> raceResponse = meetingRepository.findByRaceTypeBetweenDate(startTime, endTime).map(r -> {
            r.setSideName(SIDE_NAME_PREFIX + r.getNumber() + " " + r.getMeetingName());
            return r;
        });
        return raceResponse.sort(Comparator.comparing(RaceResponseDTO::getDate));
    }

}
