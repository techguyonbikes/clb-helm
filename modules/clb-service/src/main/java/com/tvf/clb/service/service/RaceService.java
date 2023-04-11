package com.tvf.clb.service.service;

import com.tvf.clb.base.dto.RaceEntrantDto;
import com.tvf.clb.base.dto.EntrantResponseDto;
import com.tvf.clb.base.dto.RaceBaseResponseDTO;
import com.tvf.clb.base.dto.RaceResponseDto;
import com.tvf.clb.base.dto.RaceResponseMapper;
import com.tvf.clb.base.entity.Race;
import com.tvf.clb.base.entity.RaceSite;
import com.tvf.clb.service.repository.EntrantRepository;
import com.tvf.clb.service.repository.MeetingRepository;
import com.tvf.clb.service.repository.RaceRepository;
import com.tvf.clb.service.repository.RaceSiteRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.*;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class RaceService {

    @Autowired
    private RaceRepository raceRepository;

    @Autowired
    private MeetingRepository meetingRepository;

    @Autowired
    private RaceRedisService raceRedisService;

    @Autowired
    private RaceSiteRepository raceSiteRepository;

    @Autowired
    private EntrantRepository entrantRepository;

    @Autowired
    private EntrantService entrantService;

    private static final String SIDE_NAME_PREFIX = "R";

    public Mono<Race> getRaceById(Long raceId) {
        return raceRepository.findById(raceId);
    }

    public Mono<RaceResponseDto> getRaceNewDataById(Long raceId) {

        return raceRedisService.findByRaceId(raceId)
                .switchIfEmpty(raceSiteRepository.getAllByGeneralRaceId(raceId).collectList().flatMap(raceSites -> {
                    Map<Integer, String> mapRaceSiteToUUID = raceSites.stream().collect(Collectors.toMap(RaceSite::getSiteId, RaceSite::getRaceSiteId));
                    return getRaceById(raceId)
                            .flatMap(race -> entrantRepository.getAllByRaceId(raceId)
                                    .collectList()
                                    .map(entrants -> RaceResponseMapper.toraceResponseDto(race, mapRaceSiteToUUID, entrants)));
        }));
    }

    public Flux<RaceBaseResponseDTO> searchRaces(LocalDate date, List<Long> meetingIds, List<RaceType> raceTypes) {
        List<String> raceType = raceTypes.stream()
                .map(RaceType::toString)
                .collect(Collectors.toList());
        Flux<RaceBaseResponseDTO> raceResponse;
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
        return raceResponse.sort(Comparator.comparing(RaceBaseResponseDTO::getDate));
    }

    public Flux<Race> findAllRacesInSameMeetingByRaceId(Long raceId) {
        Mono<Race> raceMono = raceRepository.findById(raceId);
        return raceMono.flatMapMany(race -> raceRepository.findAllByMeetingId(race.getMeetingId()));
    }

    public Mono<RaceEntrantDto> getRaceEntrantByRaceId(Long raceId) {
        Flux<EntrantResponseDto> entrantFlux = entrantService.getEntrantsByRaceId(raceId).switchIfEmpty(Flux.empty());
        Mono<RaceEntrantDto> raceMeetingFlux = raceRepository.getRaceEntrantByRaceId(raceId).switchIfEmpty(Mono.empty());
        Flux<Race> raceNumberId = raceRepository.getRaceIDNumberByRaceId(raceId).switchIfEmpty(Mono.empty());
        Flux<RaceSite> raceSiteUUID = raceSiteRepository.getAllByGeneralRaceId(raceId).switchIfEmpty(Mono.empty());

        return Mono.zip(entrantFlux.collectList(), raceMeetingFlux,
                        raceNumberId.collectMap(Race::getNumber, Race::getId),
                        raceSiteUUID.collectMap(RaceSite::getSiteId, RaceSite::getRaceSiteId))
                .map(tuple -> {
                    RaceEntrantDto raceEntrantDTO = tuple.getT2();

                    raceEntrantDTO.setEntrants(tuple.getT1());
                    raceEntrantDTO.setRaceIdNumber(tuple.getT3());
                    raceEntrantDTO.setRaceSiteUUID(tuple.getT4());

                    return raceEntrantDTO;
                });

    }


    public Flux<RaceBaseResponseDTO> getListRaceDefault(LocalDate date) {
        LocalDateTime maxDateTime = date.plusDays(3).atTime(23, 59, 59);
        LocalDateTime minDateTime = date.plusDays(-3).atTime(00, 00, 00);
        Instant endTime = maxDateTime.atOffset(ZoneOffset.UTC).toInstant();
        Instant startTime = minDateTime.atOffset(ZoneOffset.UTC).toInstant();
        Flux<RaceBaseResponseDTO> raceResponse = meetingRepository.findByRaceTypeBetweenDate(startTime, endTime).map(r -> {
            r.setSideName(SIDE_NAME_PREFIX + r.getNumber() + " " + r.getMeetingName());
            return r;
        });
        return raceResponse.sort(Comparator.comparing(RaceBaseResponseDTO::getDate));
    }

}
