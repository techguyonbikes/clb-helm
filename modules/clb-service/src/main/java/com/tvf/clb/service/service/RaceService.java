package com.tvf.clb.service.service;

import com.tvf.clb.base.dto.RaceEntrantDto;
import com.tvf.clb.base.dto.EntrantResponseDto;
import com.tvf.clb.base.dto.RaceBaseResponseDTO;
import com.tvf.clb.base.dto.RaceResponseDto;
import com.tvf.clb.base.dto.RaceResponseMapper;
import com.tvf.clb.base.entity.Race;
import com.tvf.clb.base.entity.RaceSite;
import com.tvf.clb.base.utils.CommonUtils;
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
import java.util.*;
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
                                    .map(entrants -> RaceResponseMapper.toRaceResponseDto(race, mapRaceSiteToUUID, entrants)));
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
        if (raceId == null){
            return null;
        }
        Flux<EntrantResponseDto> entrantFlux = entrantService.getEntrantsByRaceId(raceId).switchIfEmpty(Flux.empty());
        Mono<RaceEntrantDto> raceMeetingFlux = raceRepository.getRaceEntrantByRaceId(raceId).switchIfEmpty(Mono.empty());
        Flux<Race> raceNumberId = raceRepository.getRaceIDNumberByRaceId(raceId).switchIfEmpty(Mono.empty());
        Flux<RaceSite> raceSiteUUID = raceSiteRepository.getAllByGeneralRaceId(raceId).switchIfEmpty(Mono.empty());
        Mono<Map<Long, String>> raceIDAndRaceStatus = mapRaceIdAndStatusFromDbOrRedis(Collections.singletonList(raceId)).switchIfEmpty(Mono.empty());

        return Mono.zip(entrantFlux.collectList(), raceMeetingFlux,
                        raceNumberId.collectMap(Race::getNumber, Race::getId),
                        raceSiteUUID.collectMap(RaceSite::getSiteId, RaceSite::getRaceSiteId),
                        raceNumberId.collectMap(Race::getId, Race::getResultsDisplay),
                        raceIDAndRaceStatus)
                .map(tuple -> {
                    RaceEntrantDto raceEntrantDTO = tuple.getT2();
                    raceEntrantDTO.setStatus(tuple.getT6().get(raceId));
                    raceEntrantDTO.setFinalResult(CommonUtils.getMapRaceFinalResultFromJsonb(tuple.getT5().get(raceId)));
                    raceEntrantDTO.setEntrants(tuple.getT1());
                    raceEntrantDTO.setRaceIdNumber(tuple.getT3());
                    raceEntrantDTO.setRaceSiteUUID(tuple.getT4());

                    return raceEntrantDTO;
                });

    }


    public Flux<RaceBaseResponseDTO> getListRaceDefault(LocalDate date) {

        Instant nowTimeMin = Instant.now().atZone(ZoneOffset.UTC).with(LocalTime.MIN).toInstant();
        Instant nowTimeMax = Instant.now().atZone(ZoneOffset.UTC).with(LocalTime.MAX).toInstant();

        LocalDateTime maxDateTime = date.plusDays(3).atTime(23, 59, 59);
        LocalDateTime minDateTime = date.plusDays(-3).atTime(00, 00, 00);
        Instant endTime = maxDateTime.atOffset(ZoneOffset.UTC).toInstant();
        Instant startTime = minDateTime.atOffset(ZoneOffset.UTC).toInstant();
        Flux<RaceBaseResponseDTO> raceResponse = meetingRepository.findByRaceTypeBetweenDate(startTime, endTime).flatMap(r -> {
            r.setSideName(SIDE_NAME_PREFIX + r.getNumber() + " " + r.getMeetingName());
            if (r.getDate().isAfter(nowTimeMin) && r.getDate().isBefore(nowTimeMax)) {
                return mapRaceIdAndStatusFromDbOrRedis(Collections.singletonList(r.getId())).map(i -> {
                    r.setStatus(i.getOrDefault(r.getId(), null));
                    return r;
                });
            }
            return Flux.just(r);
        });
        return raceResponse.sort(Comparator.comparing(RaceBaseResponseDTO::getDate));
    }

    public Mono<Map<Long, String>> mapRaceIdAndStatusFromDbOrRedis(List<Long> idList) {
        if (CollectionUtils.isEmpty(idList)){
            return Mono.just(Collections.emptyMap());
        }

        Mono<List<RaceResponseDto>> raceResponseDtoMono = raceRedisService.findAllByKeysRaceResponseDto(idList);

        return raceResponseDtoMono.flatMap(response -> {
            List<RaceResponseDto> raceResponseDtoList = response.stream()
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            if (!raceResponseDtoList.isEmpty()) {
                List<Long> raceIdList = compareListId(raceResponseDtoList.stream()
                                .map(RaceResponseDto::getId)
                                .collect(Collectors.toList()),
                        idList);
                return raceIdList.isEmpty() ? Mono.just(raceResponseDtoList.stream()
                        .collect(Collectors.toMap(RaceResponseDto::getId,
                                RaceResponseDto::getStatus)))
                        : zipDataFromListRaceDbAndRedis(raceIdList, raceResponseDtoList);
            }

            return raceRepository.findAllById(idList)
                    .collectList()
                    .map(raceResponseDto -> raceResponseDto.stream()
                            .collect(Collectors.toMap(Race::getId,
                                    Race::getStatus)));
        });
    }

    public List<Long> compareListId(List<Long> listIds1, List<Long> listIds2) {
        if (CollectionUtils.isEmpty(listIds1) || CollectionUtils.isEmpty(listIds2)) {
            return Collections.emptyList();
        }
        return listIds2.stream().filter(x -> !listIds1.contains(x)).collect(Collectors.toList());
    }

    public Mono<Map<Long, String>> zipDataFromListRaceDbAndRedis(List<Long> raceIdList, List<RaceResponseDto> raceResponseDtoList) {
        if (CollectionUtils.isEmpty(raceIdList) || CollectionUtils.isEmpty(raceResponseDtoList)) {
            return Mono.just(Collections.emptyMap());
        }
        return Mono.zip(raceRepository.findAllById(raceIdList).collectList(), Mono.just(raceResponseDtoList))
                .map(tuple -> {
                    Map<Long, String> map = tuple.getT1().stream().collect(Collectors.toMap(Race::getId, Race::getStatus));
                    map.putAll(tuple.getT2().stream().collect(Collectors.toMap(RaceResponseDto::getId, RaceResponseDto::getStatus)));
                    return map;
                });
    }
}
