package com.tvf.clb.service.service;

import com.tvf.clb.base.dto.MeetingMapper;
import com.tvf.clb.base.entity.*;
import com.tvf.clb.service.repository.EntrantSiteRepository;
import com.tvf.clb.service.repository.MeetingSiteRepository;
import com.tvf.clb.service.repository.RaceSiteRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CrawUtils {

    @Autowired
    private MeetingSiteRepository meetingSiteRepository;

    @Autowired
    private RaceSiteRepository raceSiteRepository;

    @Autowired
    private EntrantSiteRepository entrantSiteRepository;

    public void saveMeetingSite(List<Meeting> meetings, Integer site) {
        List<MeetingSite> newMeetingSites = meetings.stream().map(x -> MeetingMapper.toMeetingSite(x, site)).collect(Collectors.toList());
        Flux<MeetingSite> existedMeetingSite = meetingSiteRepository
                .findAllByMeetingSiteIdInAndSiteId(meetings.stream().map(Meeting::getMeetingId).collect(Collectors.toList()), site);
        existedMeetingSite.collectList().subscribe(existed -> {
            newMeetingSites.addAll(existed);
            List<MeetingSite> meetingSiteNeedUpdateOrInsert = newMeetingSites.stream().distinct().peek(e ->
            {
                if (e.getId() == null) {
                    existed.stream()
                            .filter(x -> x.getMeetingSiteId().equals(e.getMeetingSiteId()) && x.getSiteId().equals(e.getSiteId()))
                            .findFirst()
                            .ifPresent(entrant -> e.setId(entrant.getId()));
                }
            }).filter(e -> !existed.contains(e)).collect(Collectors.toList());
            meetingSiteRepository.saveAll(meetingSiteNeedUpdateOrInsert).subscribe();
        });
    }

    public void saveRaceSite(List<Race> races, Integer site) {
        List<RaceSite> newRaceSites = races.stream().map(x -> MeetingMapper.toRaceSite(x, site)).collect(Collectors.toList());
        Flux<RaceSite> existedRaceSite = raceSiteRepository
                .findAllByRaceSiteIdInAndSiteId(newRaceSites.stream().map(RaceSite::getRaceSiteId).collect(Collectors.toList()), site);
        existedRaceSite.collectList().subscribe(existed -> {
            newRaceSites.addAll(existed);
            List<RaceSite> raceSiteNeedUpdateOrInsert = newRaceSites.stream().distinct().peek(e ->
            {
                if (e.getId() == null) {
                    existed.stream()
                            .filter(x -> x.getRaceSiteId().equals(e.getRaceSiteId()) && x.getSiteId().equals(e.getSiteId()))
                            .findFirst()
                            .ifPresent(entrant -> e.setId(entrant.getId()));
                }
            }).filter(e -> !existed.contains(e)).collect(Collectors.toList());
            raceSiteRepository.saveAll(raceSiteNeedUpdateOrInsert).subscribe();
        });
    }

    public void saveEntrantSite(List<Entrant> entrants, Integer site) {
        List<EntrantSite> newEntrantSites = entrants.stream().map(x -> MeetingMapper.toEntrantSite(x, site)).collect(Collectors.toList());
        Flux<EntrantSite> existedEntrantSite = entrantSiteRepository
                .findAllByEntrantSiteIdInAndSiteId(newEntrantSites.stream().map(EntrantSite::getEntrantSiteId).collect(Collectors.toList()), site);
        existedEntrantSite.collectList().subscribe(existed -> {
            newEntrantSites.addAll(existed);
            List<EntrantSite> raceSiteNeedUpdateOrInsert = newEntrantSites.stream().distinct().peek(e ->
            {
                if (e.getId() == null) {
                    existed.stream()
                            .filter(x -> x.getEntrantSiteId().equals(e.getEntrantSiteId()) && x.getSiteId().equals(e.getSiteId()))
                            .findFirst()
                            .ifPresent(entrant -> e.setId(entrant.getId()));
                }
            }).filter(e -> !existed.contains(e)).collect(Collectors.toList());
            entrantSiteRepository.saveAll(raceSiteNeedUpdateOrInsert).subscribe();
        });
    }

}
