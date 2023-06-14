package com.tvf.clb.service.repository;

import com.tvf.clb.base.dto.RaceEntrantDto;
import com.tvf.clb.base.entity.Race;
import io.r2dbc.postgresql.codec.Json;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

public interface RaceRepository extends R2dbcRepository<Race, Long> {

    Flux<Race> findAllById(List<Long> ids);
    Flux<Race> findAllByAdvertisedStartBetweenAndStatusNotIn(Instant start, Instant end, List<String> statuses);

    Flux<Race> findAllByNumberInAndMeetingIdIn(List<Integer> number, List<Long> meetingIds);

    @Query("Update clb_db.race set distance =:distance  WHERE id =:raceId")
    Mono<Race> setUpdateRaceDistanceById(@Param("raceId") Long raceId, @Param("distance") Integer distance);

    @Query("Update clb_db.race set results_display = :result WHERE id = :raceId")
    Mono<Race> updateRaceFinalResultById(@Param("result") Json result, @Param("raceId") Long raceId);

    @Query("Update clb_db.race set status =:status  WHERE id =:raceId")
    Mono<Race> setUpdateRaceStatusById(@Param("raceId") Long raceId, @Param("status") String status);

    Flux<Race> findAllByMeetingId(Long meetingId);

    Mono<Race> getRaceByMeetingIdInAndNumberAndAdvertisedStart(List<Long> meetingIds, Integer number, Instant advertisedStart);


    @Query("select * from clb_db.race r join clb_db.meeting m ON r.meeting_id = m.id where m.race_type =:meetingType and r.number = :number and r.advertised_start between :startTime and :endTime")
    Flux<Race> getRaceByTypeAndNumberAndRangeAdvertisedStart(@Param("meetingType") String meetingType, @Param("number") Integer number,
                                                             @Param("startTime") Instant startTime, @Param("endTime") Instant endTime);

    @Query("select r.id, r.meeting_id, m.name as meeting_name, m.race_type, m.state, r.advertised_start, r.actual_start, r.name, r.number, r.distance, r.status, m.country from clb_db.race r join clb_db.meeting m on m.id = r.meeting_id where r.id = :raceId")
    Mono<RaceEntrantDto> getRaceEntrantByRaceId(@Param("raceId") Long raceId);

    @Query("select * from clb_db.race s where s.meeting_id = (select rs.meeting_id from clb_db.race rs where rs.id = :raceId)")
    Flux<Race> getRaceIDNumberByRaceId(@Param("raceId") Long raceId);


    @Query("delete from clb_db.race r where r.advertised_start between :startTime and :endTime")
    Mono<Long> deleteAllByAdvertisedStartBetween(@Param("startTime") Instant startTime, @Param("endTime") Instant endTime);

    @Query("Update clb_db.race set status = :status, results_display = :result WHERE id =:raceId")
    Mono<Race> updateRaceStatusAndFinalResultById(@Param("raceId") Long raceId, @Param("status") String status, @Param("result") Json result);

    @Query("Update clb_db.race set advertised_start = :advertisedStart WHERE id =:raceId")
    Mono<Boolean> updateRaceAdvertisedStartById(@Param("raceId") Long raceId, @Param("advertisedStart") Instant advertisedStart);
}