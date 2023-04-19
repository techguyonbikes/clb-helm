package com.tvf.clb.scheduler;

import com.tvf.clb.base.utils.AppConstant;
import com.tvf.clb.service.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Slf4j
@Component
public class RemoveDataScheduler {

    private final MeetingRepository meetingRepository;

    private final MeetingSiteRepository meetingSiteRepository;

    private final RaceRepository raceRepository;

    private final RaceSiteRepository raceSiteRepository;

    private final EntrantRepository entrantRepository;

    public RemoveDataScheduler(MeetingRepository meetingRepository, MeetingSiteRepository meetingSiteRepository,
                               RaceRepository raceRepository, RaceSiteRepository raceSiteRepository,
                               EntrantRepository entrantRepository) {
        this.meetingRepository = meetingRepository;
        this.meetingSiteRepository = meetingSiteRepository;
        this.raceRepository = raceRepository;
        this.raceSiteRepository = raceSiteRepository;
        this.entrantRepository = entrantRepository;
    }

    /**
     * Delete all data after 7 day at 00.00AM every day.
     */
    @Scheduled(cron = "${scheduler.remove-data.cron}")
    public void dailyRemoveData() {

        log.info("Start remove all data in meeting, meeting site, race, race site, entrant after {} day", AppConstant.DATE_REMOVE_DATA);

        LocalDateTime maxDateTime = LocalDate.now().plusDays(-AppConstant.DATE_REMOVE_DATA).atTime(AppConstant.HOUR_TIME, AppConstant.MINUTE_TIME, AppConstant.SECOND_TIME);
        LocalDateTime minDateTime = LocalDate.now().plusDays(-AppConstant.DATE_REMOVE_DATA).atTime(AppConstant.ZERO_TIME, AppConstant.ZERO_TIME, AppConstant.ZERO_TIME);
        Instant endTime = maxDateTime.atOffset(ZoneOffset.UTC).toInstant();
        Instant startTime = minDateTime.atOffset(ZoneOffset.UTC).toInstant();

        entrantRepository.deleteAllByAdvertisedStartBetween(startTime, endTime)
                .doFinally(m ->
                        raceRepository.deleteAllByAdvertisedStartBetween(startTime, endTime)
                                .subscribe())
                .subscribe();

        raceSiteRepository.deleteAllByStartDateBetween(startTime, endTime).subscribe();
        meetingSiteRepository.deleteAllByStartDateBetween(startTime, endTime).subscribe();
        meetingRepository.deleteAllByAdvertisedDateBetween(startTime, endTime).subscribe();

    }

}
