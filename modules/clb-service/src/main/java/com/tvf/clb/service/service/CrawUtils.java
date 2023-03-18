package com.tvf.clb.service.service;

import com.tvf.clb.base.dto.MeetingMapper;
import com.tvf.clb.base.dto.RaceResponseMapper;
import com.tvf.clb.base.entity.*;
import com.tvf.clb.service.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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
    @Autowired
    private MeetingRepository meetingRepository;

    @Autowired
    private RaceRepository raceRepository;


//    public void saveMeetingSite(List<Meeting> meetings, Integer site) {
//        List<MeetingSite> newMeetingSites = meetings.stream().map(x -> MeetingMapper.toMeetingSite(x, site)).collect(Collectors.toList());
//        Flux<MeetingSite> existedMeetingSite = meetingSiteRepository
//                .findAllByMeetingSiteIdInAndSiteId(meetings.stream().map(Meeting::getMeetingId).collect(Collectors.toList()), site);
//        existedMeetingSite.collectList().subscribe(existed -> {
//            newMeetingSites.addAll(existed);
//            List<MeetingSite> meetingSiteNeedUpdateOrInsert = newMeetingSites.stream().distinct().peek(e ->
//            {
//                if (e.getId() == null) {
//                    existed.stream()
//                            .filter(x -> x.getMeetingSiteId().equals(e.getMeetingSiteId()) && x.getSiteId().equals(e.getSiteId()))
//                            .findFirst()
//                            .ifPresent(entrant -> e.setId(entrant.getId()));
//                }
//            }).filter(e -> !existed.contains(e)).collect(Collectors.toList());
//            meetingSiteRepository.saveAll(meetingSiteNeedUpdateOrInsert).subscribe();
//        });
//    }

//    public void saveRaceSite(List<Race> races, Integer site) {
//        List<RaceSite> newRaceSites = races.stream().map(x -> MeetingMapper.toRaceSite(x, site)).collect(Collectors.toList());
//        Flux<RaceSite> existedRaceSite = raceSiteRepository
//                .findAllByRaceSiteIdInAndSiteId(newRaceSites.stream().map(RaceSite::getRaceSiteId).collect(Collectors.toList()), site);
//        existedRaceSite.collectList().subscribe(existed -> {
//            newRaceSites.addAll(existed);
//            List<RaceSite> raceSiteNeedUpdateOrInsert = newRaceSites.stream().distinct().peek(e ->
//            {
//                if (e.getId() == null) {
//                    existed.stream()
//                            .filter(x -> x.getRaceSiteId().equals(e.getRaceSiteId()) && x.getSiteId().equals(e.getSiteId()))
//                            .findFirst()
//                            .ifPresent(entrant -> e.setId(entrant.getId()));
//                }
//            }).filter(e -> !existed.contains(e)).collect(Collectors.toList());
//            raceSiteRepository.saveAll(raceSiteNeedUpdateOrInsert).subscribe();
//        });
//    }

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
    public void saveMeetingSite(List<Meeting> meetings, Integer site) {
        Flux<MeetingSite> newMeetingSite = Flux.fromIterable(meetings).flatMap(
                r -> {
                    Mono<Long> generalId = meetingRepository.getMeetingId(r.getName(),r.getRaceType(), r.getAdvertisedDate());
                    return Flux.from(generalId).map(id -> MeetingMapper.toMetingSite(r, site, id));
                }
        );
        Flux<MeetingSite> existedMeetingSite = meetingSiteRepository
                .findAllByMeetingSiteIdInAndSiteId(meetings.stream().map(Meeting::getMeetingId).collect(Collectors.toList()), site);

        Flux.zip(newMeetingSite.collectList(), existedMeetingSite.collectList())
                .doOnNext(tuple2 -> {
                    tuple2.getT2().forEach(dup -> tuple2.getT1().remove(dup));
                    log.info("Meeting site " + site + " need to be update is " + tuple2.getT1().size());
                    meetingSiteRepository.saveAll(tuple2.getT1()).subscribe();
                }).subscribe();
    }

    public void saveRaceSite(List<Race> races, Integer site) {
        Flux<RaceSite> newMeetingSite = Flux.fromIterable(races.stream().filter(x -> x.getNumber() != null).collect(Collectors.toList())).flatMap(
                race -> {
                    Mono<Long> generalId = raceRepository.getRaceId(race.getMeetingId(),race.getNumber(), race.getAdvertisedStart());
                    return Flux.from(generalId).map(id -> RaceResponseMapper.toRacesiteDto(race, site, id));
                }
        );
        Flux<RaceSite> existedMeetingSite = raceSiteRepository
                .findAllByRaceSiteIdInAndSiteId(races.stream().map(Race::getRaceId).collect(Collectors.toList()), site);

        Flux.zip(newMeetingSite.collectList(), existedMeetingSite.collectList())
                .doOnNext(tuple2 -> {
                    tuple2.getT2().forEach(dup -> tuple2.getT1().remove(dup));
                    log.info("Race site "  + site + " need to be update is " + tuple2.getT1().size());
                    raceSiteRepository.saveAll(tuple2.getT1()).subscribe();
                }).subscribe();

    }


}
