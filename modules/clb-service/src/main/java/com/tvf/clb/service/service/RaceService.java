package com.tvf.clb.service.service;

import com.tvf.clb.base.dto.*;
import com.tvf.clb.base.entity.Race;
import com.tvf.clb.base.entity.RaceSite;
import com.tvf.clb.base.utils.AppConstant;
import com.tvf.clb.base.utils.CommonUtils;
import com.tvf.clb.base.utils.ConvertBase;
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
import java.util.function.Function;
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


    public Mono<Race> getRaceById(Long raceId) {
        return raceRepository.findById(raceId);
    }

    public Mono<RaceResponseDto> getRaceNewDataById(Long raceId) {

        return raceRedisService.findByRaceId(raceId)
                .switchIfEmpty(raceSiteRepository.getAllByGeneralRaceId(raceId).collectList().flatMap(raceSites -> {
                    Map<Integer, String> mapRaceSiteToUUID = raceSites.stream().collect(Collectors.toMap(RaceSite::getSiteId, RaceSite::getRaceSiteId, (first, second) -> first))   ;
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
                        r.setSideName(ConvertBase.getSideName(r));
                        return r;
                    });

        } else {
            raceResponse = meetingRepository.findByRaceTypeAndMeetingId(raceType, meetingIds, startDate)
                    .map(r -> {
                        r.setSideName(ConvertBase.getSideName(r));
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
        Mono<Map<Long, RaceResponseDto>> mapMonoRaceProperty = getNewestRaceProperty(Collections.singletonList(raceId), Function.identity(), RaceResponseMapper::toRaceResponseDto).switchIfEmpty(Mono.empty());

        return Mono.zip(entrantFlux.collectList(),
                        raceMeetingFlux,
                        raceNumberId.collectMap(Race::getNumber, Race::getId),
                        raceSiteUUID.collectMap(RaceSite::getSiteId, RaceSite::getRaceSiteId),
                        raceNumberId.collectMap(Race::getId, Race::getResultsDisplay),
                        mapMonoRaceProperty,
                        raceSiteUUID.collectMap(RaceSite::getSiteId, RaceSite::getRaceSiteUrl))
                .map(tuple -> {
                    RaceResponseDto raceDataInDbOrRedis = tuple.getT6().get(raceId);
                    RaceEntrantDto raceEntrantDTO = tuple.getT2();
                    raceEntrantDTO.setStatus(tuple.getT6().get(raceId).getStatus());
                    raceEntrantDTO.setFinalResult(tuple.getT6().get(raceId).getFinalResult());
                    raceEntrantDTO.setEntrants(tuple.getT1());
                    raceEntrantDTO.setRaceIdNumber(tuple.getT3());
                    raceEntrantDTO.setRaceSiteUUID(tuple.getT4());
                    raceEntrantDTO.setRaceSiteUrl(tuple.getT7());

                    Instant advertisedStart = raceDataInDbOrRedis.getAdvertisedStart() == null ? raceEntrantDTO.getAdvertisedStart() : Instant.parse(raceDataInDbOrRedis.getAdvertisedStart());
                    Instant actualStart = raceDataInDbOrRedis.getActualStart() == null ? raceEntrantDTO.getActualStart() : Instant.parse(raceDataInDbOrRedis.getActualStart());

                    raceEntrantDTO.setActualStart(actualStart);
                    raceEntrantDTO.setAdvertisedStart(advertisedStart);

                    return raceEntrantDTO;
                });

    }


    public Flux<RaceBaseResponseDTO> getListRaceDefault(LocalDate date) {
        Instant startTime;
        Instant endTime;
        if (date == null){
            startTime = LocalDate.now().plusDays(-3).atTime(LocalTime.MIN).atOffset(ZoneOffset.UTC).toInstant();
            endTime = LocalDate.now().plusDays(3).atTime(LocalTime.MAX).atOffset(ZoneOffset.UTC).toInstant();
        } else {
            startTime = date.atTime(LocalTime.MIN).atOffset(ZoneOffset.UTC).toInstant();
            endTime = date.atTime(AppConstant.HOUR_TIME, AppConstant.MINUTE_TIME, AppConstant.SECOND_TIME).atOffset(ZoneOffset.UTC).toInstant();
        }

        Flux<RaceBaseResponseDTO> raceResponse = meetingRepository.findByRaceTypeBetweenDate(startTime, endTime)
        .map(race -> {
            race.setSideName(ConvertBase.getSideName(race));
            return race;
        })
        .collectList()
        .flatMap(listRace -> {
            if (listRace.isEmpty()) {
                return Mono.empty();
            }
            // Set newest status for the races still in redis
            Map<Long, RaceBaseResponseDTO> mapIdToRace = listRace.stream().collect(Collectors.toMap(RaceBaseResponseDTO::getId, Function.identity()));

            return raceRedisService.findAllByRaceIds(mapIdToRace.keySet()).map(listRaceInRedis -> {
                for (RaceResponseDto raceInRedis : listRaceInRedis) {
                    if (raceInRedis == null) continue;
                    mapIdToRace.get(raceInRedis.getId()).setStatus(raceInRedis.getStatus());
                    if (raceInRedis.getActualStart() != null){
                        mapIdToRace.get(raceInRedis.getId()).setDate(Instant.parse(raceInRedis.getActualStart()));
                    }
                }
                return mapIdToRace.values();
            }).switchIfEmpty(Mono.just(mapIdToRace.values()));

        })
        .flatMapMany(Flux::fromIterable);

        return raceResponse.sort(Comparator.comparing(RaceBaseResponseDTO::getDate));
    }

    public <T> Mono<Map<Long, T>> getNewestRaceProperty(List<Long> idList, Function<RaceResponseDto, T> getPropertyFromObjectInRedis, Function<Race, T> getPropertyFromObjectInDB) {
        if (CollectionUtils.isEmpty(idList)){
            return Mono.just(Collections.emptyMap());
        }

        // Find all races in redis by ids
        Mono<List<RaceResponseDto>> raceResponseDtoMono = raceRedisService.findAllByRaceIds(idList);

        return raceResponseDtoMono.flatMap(response -> {

            List<RaceResponseDto> racesInRedis = response.stream().filter(Objects::nonNull).collect(Collectors.toList());

            Map<Long, T> result = new HashMap<>();

            if (! racesInRedis.isEmpty()) {
                List<Long> raceIdsNotInRedis = getRaceIdsIsMissing(racesInRedis.stream().map(RaceResponseDto::getId).collect(Collectors.toList()), idList);

                if (raceIdsNotInRedis.isEmpty()) { // return if all races in redis
                    racesInRedis.forEach(race -> result.put(race.getId(), getPropertyFromObjectInRedis.apply(race)));
                    return Mono.just(result);

                } else {
                    // Need to find more in DB if the list return from redis is not enough
                    return Mono.zip(raceRepository.findAllById(raceIdsNotInRedis).collectList(), Mono.just(racesInRedis))
                            .map(tuple -> {
                                tuple.getT1().forEach(race -> result.put(race.getId(), getPropertyFromObjectInDB.apply(race)));
                                tuple.getT2().forEach(race -> result.put(race.getId(), getPropertyFromObjectInRedis.apply(race)));
                                return result;
                            });
                }
            }

            // find all in db if no race in redis
            return raceRepository.findAllById(idList)
                    .collectList()
                    .map(races -> {
                        races.forEach(race -> result.put(race.getId(), getPropertyFromObjectInDB.apply(race)));
                        return result;
                    });
        });
    }

    public List<Long> getRaceIdsIsMissing(List<Long> listIds1, List<Long> listIds2) {
        if (CollectionUtils.isEmpty(listIds1) || CollectionUtils.isEmpty(listIds2)) {
            return Collections.emptyList();
        }
        return listIds2.stream().filter(x -> !listIds1.contains(x)).collect(Collectors.toList());
    }

}
