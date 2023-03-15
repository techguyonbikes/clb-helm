package com.tvf.clb.service.repository;

import com.tvf.clb.base.dto.MeetingDto;
import com.tvf.clb.base.dto.RaceResponseDTO;
import com.tvf.clb.base.entity.Meeting;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.List;

@Repository
public interface MeetingRepository extends R2dbcRepository<Meeting, Long> {
    Mono<Meeting> findByMeetingId(String meetingId);

    Flux<Meeting> findAllByMeetingIdIn(List<String> meetingIds);

    @Query("select m.name as name, m.race_type, m.meeting_id as id, m.advertised_date\n" +
            "from clb_db.meeting m \n" +
            "where m.advertised_date::date = current_date ")
    Flux<MeetingDto> findMeetingByDate();

    @Query("SELECT r.race_id, r.number, r.actual_start as date, r.name as race_name, r.distance, m.meeting_id, m.race_type as type, m.name as meeting_name , m.state as state" +
            " FROM clb_db.meeting m JOIN clb_db.race r ON m.meeting_id = r.meeting_id" +
            " WHERE m.race_type IN (:raceTypes)" +
            " AND m.meeting_id IN (:meetingIds)" +
            " AND m.advertised_Date = :date")
    Flux<RaceResponseDTO> findByRaceTypeAndMeetingId(@Param("raceTypes") List<String> raceTypes,
                                                     @Param("meetingIds") List<String> meetingIds,
                                                     @Param("date") Instant date);

    @Query("SELECT r.race_id, r.number, r.actual_start as date, r.name as race_name, r.distance, m.meeting_id, m.race_type as type, m.name as meeting_name , m.state as state" +
            " FROM clb_db.meeting m JOIN clb_db.race r ON m.meeting_id = r.meeting_id" +
            " WHERE m.race_type IN (:raceTypes)" +
            " AND m.advertised_Date = :date")
    Flux<RaceResponseDTO> findByRaceTypes(@Param("raceTypes") List<String> raceTypes,
                                          @Param("date") Instant date);
}
