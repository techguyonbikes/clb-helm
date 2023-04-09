package com.tvf.clb.service.repository;

import com.tvf.clb.base.entity.Race;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

public interface RaceRepository extends R2dbcRepository<Race, Long> {
    Flux<Race> findAllByAdvertisedStartBetweenAndStatusNotIn(Instant start, Instant end, List<String> statuses);

    Flux<Race> findAllByNameInAndNumberInAndAdvertisedStartIn(@Param("name") List<String> name, @Param("number") List<Integer> number, @Param("date") List<Instant> date);

    @Query("Update clb_db.race set distance =:distance  WHERE id =:raceId")
    Mono<Race> setUpdateRaceDistanceById(@Param("raceId") Long raceId, @Param("distance") Integer distance);

    @Query("Update clb_db.race set status =:status  WHERE id =:raceId")
    Mono<Race> setUpdateRaceStatusById(@Param("raceId") Long raceId, @Param("status") String status);

    Flux<Race> findAllByMeetingId(Long meetingId);

    Mono<Race> getRaceByMeetingIdInAndNumberAndAdvertisedStart(List<Long> meetingIds, Integer number, Instant advertisedStart);


    @Query("select r.id from clb_db.race r join clb_db.meeting m ON r.meeting_id = m.id where m.race_type =:meetingType and r.number = :number and r.advertised_start = :date")
    Mono<Long> getRaceIdByMeetingType(@Param("meetingType") String meetingType, @Param("number") Integer number, @Param("date") Instant date);

    @Query("select r.id from clb_db.race r where r.distance = :distance and r.number = :number and r.advertised_start = :date")
    Mono<Long> getRaceIdbyDistance(@Param("distance") int name, @Param("number") Integer number, @Param("date") Instant date);
}