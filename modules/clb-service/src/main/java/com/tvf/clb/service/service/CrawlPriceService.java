package com.tvf.clb.service.service;

import com.google.gson.JsonObject;
import com.tvf.clb.base.dto.EntrantMapper;
import com.tvf.clb.base.dto.LadBrokedItRaceDto;
import com.tvf.clb.base.dto.MeetingMapper;
import com.tvf.clb.base.entity.*;
import com.tvf.clb.base.model.EntrantSiteRawData;
import com.tvf.clb.base.model.EntrantRawData;
import com.tvf.clb.base.model.RaceEntrantDTO;
import com.tvf.clb.service.repository.EntrantRepository;
import com.tvf.clb.service.repository.EntrantSiteRepository;
import com.tvf.clb.service.repository.RaceSiteRepository;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class CrawlPriceService {

    private final org.slf4j.Logger log = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private EntrantRepository entrantRepository;

    @Autowired
    private CrawlService crawlService;

    @Autowired
    private RaceSiteRepository raceSiteRepository;

    @Autowired
    private EntrantSiteRepository entrantSiteRepository;

    @Autowired
    private LadBrokeCrawlService ladBrokeCrawlService;

    @Autowired
    private CrawUtils crawUtils;

    @Autowired
    private EntrantRedisService entrantRedisService;

    public Mono<List<EntrantRedis>> crawlPriceByRaceId(Long generalRaceId) {

        return raceSiteRepository.getAllByGeneralRaceId(generalRaceId)
                .parallel()
                .runOn(Schedulers.parallel())
                .map(this::getEntrantByRaceId)
                .sequential()
                .collectList() // collect all responses data into a list after crawls from all sites
                .map(crawledList -> saveEntrantForRacing(crawledList, generalRaceId)); // save all crawled data and return corresponding entrants, race info
    }

    /**
     * This function crawl data for All API
     *
     * @param raceSiteDTO
     *
     * @return
     */
    public RaceEntrantDTO getEntrantByRaceId(RaceSite raceSiteDTO) {
        try {
            LadBrokedItRaceDto raceDto = new LadBrokedItRaceDto();
            switch (raceSiteDTO.getSiteId()) {
                case 1:
                    log.info("call API to site: LADBROKES");
                    raceDto = ladBrokeCrawlService.getLadBrokedItRaceDto(raceSiteDTO.getRaceSiteId());
                    break;
                case 2:
                    log.info("ZBET");
                case 3:
                    log.info("SPORTSNET");
                case 4:
                    log.info("call API to site: NEDS");
                    raceDto = crawlService.getLadBrokedItRaceDto(raceSiteDTO.getRaceSiteId());
                    break;
                case 5:
                    log.info("POINTSBET");
                case 6:
                    log.info("TOPSPORT");
                case 7:
                    log.info("BET365");
                case 8:
                    log.info("TAB");
                default:
                    log.info("DONE!!!");
            }
            JsonObject results = raceDto.getResults();
            Map<String, Integer> positions = new HashMap<>();
            String statusRace = null;
            if (results != null) {
                positions = results.keySet().stream().collect(Collectors.toMap(Function.identity(), key -> results.getAsJsonObject(key).get("position").getAsInt()));
                statusRace = String.valueOf(Race.Status.F);
            } else {
                positions.put("position", 0);
                statusRace = String.valueOf(Race.Status.O);
            }
//            String distance = raceDto.getRaces().getAsJsonObject(raceSiteDTO.getRaceSiteId()).getAsJsonObject("additional_info").get("distance").getAsString();
//            raceRepository.setUpdateRaceByRaceId(raceSiteDTO.getRaceSiteId(), distance == null ? 0 : Integer.parseInt(distance), statusRace).subscribe();
            HashMap<String, ArrayList<Float>> allEntrantPrices = raceDto.getPriceFluctuations();
            List<EntrantRawData> allEntrant = getListEntrant(raceDto, allEntrantPrices, raceSiteDTO.getRaceSiteId(), positions);
            return new RaceEntrantDTO(statusRace, allEntrant, raceSiteDTO.getSiteId(), raceSiteDTO.getGeneralRaceId());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * This function update entrants price and race status into Redis or DB
     *
     * @param priceDTOS all crawled data include race status, entrants info
     * @param raceId    the race id
     * @return the entrants price and race status
     */
    public List<EntrantRedis> saveEntrantForRacing(List<RaceEntrantDTO> priceDTOS, Long raceId) {

        List<EntrantSiteRawData> entrantSiteRawData = priceDTOS.stream()
                .flatMap(priceDTO -> priceDTO.getAllEntrant()
                        .stream()
                        .map(entrant -> EntrantMapper.mapPrices(entrant, priceDTO.getSiteId(), priceDTO.getStatusRace())))
                .collect(Collectors.toList()); // flat to stream of EntrantPriceRawData and collect all into a list

        // Map list EntrantPriceRawData to list EntrantRedis for saving
        Map<EntrantSiteRawData, Map<Integer, List<Float>>> result = entrantSiteRawData.stream().collect(
                Collectors.groupingBy(Function.identity(), Collectors.toMap(EntrantSiteRawData::getSiteId, EntrantSiteRawData::getPriceFluctuations)));
        List<EntrantRedis> entrantRedis = new ArrayList<>();
        result.keySet().forEach(
                key -> entrantRedis.add(new EntrantRedis(key.getId(), raceId, key.getName(), key.getNumber(), key.getMarketId(), key.getStatus(), result.get(key)))
        );

        String raceStatus = priceDTOS.get(0).getStatusRace();

        if (raceStatus.equals(String.valueOf(Race.Status.O))) { // check crawled races is opening or not, if true save all entrants to Redis
            entrantRedisService.saveAll(entrantRedis).subscribe();

        } else { // else save all to DB and remove all from Redis
            entrantRedisService.delete(raceId).subscribe();
            priceDTOS.forEach(priceDTO -> saveEntrant(priceDTO.getAllEntrant(), priceDTO.getSiteId()));
        }

        return entrantRedis;
    }

    public List<EntrantRawData> getListEntrant(LadBrokedItRaceDto raceDto, HashMap<String, ArrayList<Float>> allEntrantPrices, String raceId, Map<String, Integer> positions) {
        return raceDto.getEntrants().values().stream().filter(r -> r.getFormSummary() != null && r.getId() != null).map(r -> {
            List<Float> entrantPrices = allEntrantPrices == null ? new ArrayList<>() : allEntrantPrices.get(r.getId());
            Integer entrantPosition = positions.get(r.getId()) == null ? 0 : positions.get(r.getId());
            EntrantRawData entrantRawData = EntrantMapper.mapPrices(r, entrantPrices, entrantPosition);
            entrantRawData.setRaceId(raceId);
            return entrantRawData;
        }).collect(Collectors.toList());
    }

    public void saveEntrant(List<EntrantRawData> entrantRawData, Integer siteId) {
        List<Entrant> newEntrants = entrantRawData.stream().map(MeetingMapper::toEntrantEntity).collect(Collectors.toList());
        Flux<Long> entrantIds = entrantSiteRepository
                .findAllByEntrantSiteIdIn(newEntrants.stream()
                        .map(Entrant::getEntrantId).collect(Collectors.toList()))
                .map(EntrantSite::getGeneralEntrantId);
        entrantIds.collectList().subscribe(r -> entrantRepository.findAllByIdIn(r)
                .collectList()
                .subscribe(existed ->
                        {
                            newEntrants.addAll(existed);
                            List<Entrant> entrantNeedUpdateOrInsert = newEntrants.stream().distinct().peek(e ->
                            {
                                if (e.getId() == null) {
                                    existed.stream()
                                            .filter(x -> x.getName().equals(e.getName())
                                                    && x.getNumber().equals(e.getNumber())
                                                    && x.getBarrier().equals(e.getBarrier()))
                                            .findFirst()
                                            .ifPresent(entrant -> e.setId(entrant.getId()));
                                }
                            }).filter(e -> !existed.contains(e)).collect(Collectors.toList());
                            log.info("Entrant need to be update is " + entrantNeedUpdateOrInsert.size());
                            entrantRepository.saveAll(entrantNeedUpdateOrInsert).collectList().subscribe(entrants -> crawUtils.saveEntrantSite(entrants, siteId));
                        }
                ));
    }

}
