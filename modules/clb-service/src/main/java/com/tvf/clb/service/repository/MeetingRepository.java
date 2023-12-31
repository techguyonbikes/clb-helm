package com.tvf.clb.service.repository;

import com.tvf.clb.base.dto.MeetingAndSiteUUID;
import com.tvf.clb.base.dto.MeetingOptions;
import com.tvf.clb.base.dto.RaceBaseResponseDTO;
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

    Flux<Meeting> findAllByIdIn(List<Long> ids);

    @Query("select m.name, m.race_type, m.id" +
            " from clb_db.meeting m where m.advertised_date = :date ")
    Flux<MeetingOptions> findMeetingByDate(@Param("date") Instant date);

    @Query("SELECT r.id, r.number, r.advertised_start as date, r.name as race_name, r.distance, r.meeting_id , m.race_type as type, m.name as meeting_name , m.state as state, r.status as status, m.country as country" +
            " FROM clb_db.meeting m JOIN clb_db.race r ON m.id = r.meeting_id" +
            " WHERE m.race_type IN (:raceTypes)" +
            " AND m.id IN (:meetingIds)" +
            " AND r.advertised_start = :date")
    Flux<RaceBaseResponseDTO> findByRaceTypeAndMeetingId(@Param("raceTypes") List<String> raceTypes,
                                                         @Param("meetingIds") List<Long> meetingIds,
                                                         @Param("date") Instant date);

    @Query("SELECT r.id, r.number, r.advertised_start as date, r.name as race_name, r.distance, r.meeting_id , m.race_type as type, m.name as meeting_name , m.state as state, r.status as status, m.country as country" +
            " FROM clb_db.meeting m JOIN clb_db.race r ON m.id = r.meeting_id" +
            " WHERE m.race_type IN (:raceTypes)" +
            " AND r.advertised_start = :date")
    Flux<RaceBaseResponseDTO> findByRaceTypes(@Param("raceTypes") List<String> raceTypes,
                                              @Param("date") Instant date);

    @Query("select * from clb_db.meeting m  where (:state is null or m.state = :state) and m.race_type = :raceType and m.advertised_date = :date")
    Flux<Meeting> getMeetingDiffName(@Param("state") String state, @Param("raceType") String raceType,@Param("date") Instant date);

    @Query("select * from clb_db.meeting m  where m.race_type = :raceType and m.advertised_date between :startTime and :endTime")
    Flux<Meeting> findAllMeetingByRaceTypeAndAdvertisedDate(@Param("raceType") String raceType, @Param("startTime") Instant startTime, @Param("endTime") Instant endTime);


    @Query("select m.id from clb_db.meeting m where m.name like CONCAT('%', :name, '%') and m.race_type = :raceType and m.advertised_date >= :date")
    Flux<Long> getMeetingIdsByNameContainsAndRaceTypeAndAdvertisedDateFrom(@Param("name") String name, @Param("raceType") String raceType, @Param("date") Instant date);

    @Query("SELECT r.id, r.number, r.actual_start as date, r.name as race_name, r.distance, r.meeting_id , m.race_type as type, m.name as meeting_name , m.state as state, r.status as status,m.country as country" +
            " FROM clb_db.meeting m JOIN clb_db.race r ON m.id = r.meeting_id" +
            " WHERE r.actual_start between :startTime and :endTime " )
    Flux<RaceBaseResponseDTO> findByRaceTypeBetweenDate(@Param("startTime") Instant  startTime,
                                          @Param("endTime") Instant endTime);

    @Query("delete from clb_db.meeting m where m.advertised_date between :startTime and :endTime")
    Mono<Long> deleteAllByAdvertisedDateBetween(@Param("startTime") Instant startTime, @Param("endTime") Instant endTime);

    @Query("select m.id from clb_db.meeting m where m.name = :name and m.race_type = :raceType and m.advertised_date = :date")
    Mono<Long> getMeetingIdByNameAndRaceTypeAndAdvertisedStart(@Param("name") String name, @Param("raceType") String raceType, @Param("date") Instant date);

    @Query("SELECT meeting.*, ms.meeting_site_id as site_UUID" +
            " FROM clb_db.meeting meeting JOIN clb_db.meeting_site ms ON meeting.id = ms.general_meeting_id" +
            " WHERE ms.meeting_site_id in (:uuids) AND ms.site_id = :siteId")
    Flux<MeetingAndSiteUUID> findAllByMeetingUUIDInAndSiteId(@Param("uuids") List<String> uuids, @Param("siteId") Integer siteId);
}
