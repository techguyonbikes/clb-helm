package com.tvf.clb.service.service;

import com.tvf.clb.base.dto.RaceEntrantDto;
import com.tvf.clb.base.dto.RaceResponseDTO;
import com.tvf.clb.base.entity.EntrantResponseDto;
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

    @Autowired
    private EntrantService entrantService;

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

    public Flux<RaceEntrantDto> getAllMeetingRaceByRaceId(Long raceId) {
                Flux<EntrantResponseDto> entrantFlux = entrantService.getEntrantsByRaceId(raceId).switchIfEmpty(Flux.empty());
                Flux<RaceEntrantDto> raceMeetingFlux = raceRepository.getRaceByIdAndAllMeeting(raceId).switchIfEmpty(Flux.empty());

                        return Flux.zip(entrantFlux.collectList(), raceMeetingFlux.collectList())
                                .flatMap(tuple -> {
                                List<EntrantResponseDto> entrants = tuple.getT1();
                                List<RaceEntrantDto> meetings = tuple.getT2();
                                for (RaceEntrantDto m : meetings) {
                                        if (raceId.equals(m.getId())) {
                                                m.setEntrants(entrants);
                                           }
                                    }
                                return Flux.fromIterable(meetings)
                                                .sort(Comparator.comparing(RaceEntrantDto::getNumber));
                           });
            }

}
