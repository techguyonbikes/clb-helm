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
    Flux<Race> findAllByAdvertisedStartBetweenAndStatusNotIn(Instant start, Instant end, List<String> statuses);

    Flux<Race> findAllByNameInAndNumberInAndAdvertisedStartIn(@Param("name") List<String> name, @Param("number") List<Integer> number, @Param("date") List<Instant> date);

    @Query("Update clb_db.race set distance =:distance  WHERE id =:raceId")
    Mono<Race> setUpdateRaceDistanceById(@Param("raceId") Long raceId, @Param("distance") Integer distance);

    @Query("Update clb_db.race set results_display = :result WHERE id = :raceId")
    Mono<Race> updateRaceFinalResultById(@Param("result") Json result, @Param("raceId") Long raceId);

    @Query("Update clb_db.race set status =:status  WHERE id =:raceId")
    Mono<Race> setUpdateRaceStatusById(@Param("raceId") Long raceId, @Param("status") String status);

    Flux<Race> findAllByMeetingId(Long meetingId);

    Mono<Race> getRaceByMeetingIdInAndNumberAndAdvertisedStart(List<Long> meetingIds, Integer number, Instant advertisedStart);


    @Query("select r.id from clb_db.race r join clb_db.meeting m ON r.meeting_id = m.id where m.race_type =:meetingType and r.number = :number and r.advertised_start between :startTime and :endTime and r.distance = :distanceRace")
    Mono<Long> getRaceIdByMeetingType(@Param("meetingType") String meetingType, @Param("number") Integer number,
                                      @Param("startTime") Instant startTime, @Param("endTime") Instant endTime,
                                      @Param("distanceRace") Integer distanceRace);

    @Query("select r.id from clb_db.race r where r.distance = :distance and r.number = :number and r.advertised_start = :date")
    Mono<Long> getRaceIdbyDistance(@Param("distance") int name, @Param("number") Integer number, @Param("date") Instant date);

    @Query("select s.id, s.meeting_id, m.name as meeting_name, m.race_type, m.state, s.advertised_start, s.actual_start, s.name, s.number, s.distance, s.status from clb_db.race s join clb_db.meeting m on m.id = s.meeting_id where s.id = :raceId")
    Mono<RaceEntrantDto> getRaceEntrantByRaceId(@Param("raceId") Long raceId);

    @Query("select * from clb_db.race s where s.meeting_id = (select rs.meeting_id from clb_db.race rs where rs.id = :raceId)")
    Flux<Race> getRaceIDNumberByRaceId(@Param("raceId") Long raceId);

    @Query("delete from clb_db.race r where r.advertised_start between :startTime and :endTime")
    Mono<Long> deleteAllByAdvertisedStartBetween(@Param("startTime") Instant startTime, @Param("endTime") Instant endTime);

    @Query("select r.* from clb_db.race r join clb_db.meeting m ON r.meeting_id = m.id where m.race_type = :raceType and r.number = :number and r.advertised_start between :startTime and :endTime and r.distance = :distanceRace")
    Mono<Race> getRaceByTypeAndNumberAndAdvertisedStart(@Param("raceType") String raceType, @Param("number") Integer number,
                                                        @Param("startTime") Instant startTime, @Param("endTime") Instant endTime,
                                                        @Param("distanceRace") Integer distanceRace);

    @Query("select r.id from clb_db.race r where r.advertised_start between :startTime and :endTime and r.name = :name and r.number = :number")
    Mono<Long> getRaceByNameAndNumberAndStartTime(@Param("startTime") Instant startTime, @Param("endTime") Instant endTime,
                                                  @Param("name") String name, @Param("number") Integer number);

    @Query("Update clb_db.race set status = :status, results_display = :result WHERE id =:raceId")
    Mono<Race> updateRaceStatusAndFinalResultById(@Param("raceId") Long raceId, @Param("status") String status, @Param("result") Json result);
}