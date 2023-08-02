package com.tvf.clb.service.service;

import com.tvf.clb.base.anotation.ClbService;
import com.tvf.clb.base.dto.*;
import com.tvf.clb.base.dto.topsport.TopSportEntrantDto;
import com.tvf.clb.base.dto.topsport.TopSportMeetingDto;
import com.tvf.clb.base.dto.topsport.TopSportRaceDto;
import com.tvf.clb.base.entity.Entrant;
import com.tvf.clb.base.entity.Meeting;
import com.tvf.clb.base.entity.Race;
import com.tvf.clb.base.model.CrawlEntrantData;
import com.tvf.clb.base.model.CrawlRaceData;
import com.tvf.clb.base.utils.AppConstant;
import com.tvf.clb.base.utils.ConvertBase;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;


@ClbService(componentType = AppConstant.TOP_SPORT)
@Slf4j
public class TopSportCrawlService implements ICrawlService {
    private final CrawUtils crawUtils;

    @Autowired
    public TopSportCrawlService(CrawUtils crawUtils) {
        this.crawUtils = crawUtils;
    }

    @Override
    public Flux<MeetingDto> getTodayMeetings(LocalDate date) {
        log.info("Start getting the API from TopSport.");

        CrawlMeetingFunction crawlFunction = crawlDate -> {
            Document doc = Jsoup.connect(AppConstant.TOPSPORT_MEETING_QUERY.replace(AppConstant.DATE_PARAM, ConvertBase.getDateOfWeek(date)))
                    .followRedirects(false).get();
            Elements links = doc.getElementsByTag(AppConstant.BODY);
            List<TopSportMeetingDto> topSportMeetingDtos = new ArrayList<>();
            for (Element link : links) {
                topSportMeetingDtos.addAll(getListMeetingByElement(link, date));
            }
            return getAllAusMeetings(topSportMeetingDtos, date);
        };

        return crawUtils.crawlMeeting(crawlFunction, date, 20000L, this.getClass().getName());
    }

    private List<MeetingDto> getAllAusMeetings(List<TopSportMeetingDto> meetingDtoList, LocalDate date) {
        List<MeetingDto> listMeetingDto = meetingDtoList.stream().map(MeetingMapper:: toMeetingDtoFromTOP).collect(Collectors.toList());

        Map<Meeting, List<Race>> mapMeetingAndRace = new HashMap<>();
        for (TopSportMeetingDto meeting : meetingDtoList) {
            List<Race> racesInMeeting = new ArrayList<>();
            for (String raceSiteUUID : meeting.getRaceId()) {
                TopSportRaceDto topSportRaceDto = crawlAndSaveEntrantsAndRace(meeting, raceSiteUUID, date);
                if (topSportRaceDto != null) {
                    racesInMeeting.add(MeetingMapper.toRaceEntityFromTOP(topSportRaceDto));
                }
            }
            mapMeetingAndRace.put(MeetingMapper.toMeetingEntityFromTOP(meeting), racesInMeeting);
        }

        crawUtils.saveMeetingSiteAndRaceSite(mapMeetingAndRace, SiteEnum.TOP_SPORT.getId()).subscribe();

        return listMeetingDto;
    }

    @Override
    public Mono<CrawlRaceData> getEntrantByRaceUUID(String raceId) {
        TopSportRaceDto topSportRaceDto = getRacesByTOP(raceId);
        if (topSportRaceDto == null) {
            return Mono.empty();
        }
        List<TopSportEntrantDto> allEntrant = topSportRaceDto.getRunners();
        Map<Integer, CrawlEntrantData> entrantMap = new HashMap<>();
        allEntrant.forEach(x -> {
            Map<Integer, List<Float>> priceFluctuations = new HashMap<>();
            priceFluctuations.put(AppConstant.TOPSPORT_SITE_ID, x.getPrice());
            entrantMap.put(x.getBarrier(), new CrawlEntrantData(0, priceFluctuations, null));
        });
        CrawlRaceData result = new CrawlRaceData();
        result.setSiteEnum(SiteEnum.TOP_SPORT);
        result.setMapEntrants(entrantMap);
        if (!topSportRaceDto.getResults().isEmpty()) {
            result.setFinalResult(Collections.singletonMap(AppConstant.TOPSPORT_SITE_ID, topSportRaceDto.getResults()));
        }
        return Mono.just(result);
    }

    public TopSportRaceDto crawlAndSaveEntrantsAndRace(TopSportMeetingDto meetingDto, String raceSiteUUID, LocalDate date) {
        TopSportRaceDto topSportRaceDto = getRacesByTOP(raceSiteUUID);
        if (topSportRaceDto != null) {
            topSportRaceDto.setRaceType(ConvertBase.convertRaceTypeOfTOP(meetingDto.getRaceType()));
            topSportRaceDto.setMeetingName(meetingDto.getName().toUpperCase());

            List<TopSportEntrantDto> allEntrantRawData = topSportRaceDto.getRunners();
            RaceDto raceDto = RaceResponseMapper.toRaceDTO(topSportRaceDto);

            List<Entrant> newEntrants = allEntrantRawData.stream().distinct().map(EntrantMapper::toEntrantEntity).collect(Collectors.toList());

            Mono<Long> raceIdMono = crawUtils.getIdForNewRace(raceDto, newEntrants);

            raceIdMono.subscribe(raceId -> {
                raceDto.setRaceId(raceId);
                if (!topSportRaceDto.getResults().isEmpty()) {
                    crawUtils.updateRaceFinalResultIntoDB(raceDto.getRaceId(), topSportRaceDto.getResults(), SiteEnum.TOP_SPORT.getId());
                    raceDto.setFinalResult(topSportRaceDto.getResults());
                }
                crawUtils.saveEntrantCrawlDataToRedis(newEntrants, raceDto, SiteEnum.TOP_SPORT.getId());
            });

            return topSportRaceDto;
        } else {
            crawUtils.saveFailedCrawlRaceForTopSport(meetingDto, raceSiteUUID, date);
            return null;
        }
    }

    public TopSportRaceDto getRacesByTOP(String raceId) {

        CrawlRaceFunction crawlFunction = raceUUID -> {
            TopSportRaceDto topSportRaceDto = new TopSportRaceDto();
            Document doc = Jsoup.connect(AppConstant.TOPSPORT_RACE_QUERY.replace(AppConstant.ID_PARAM, raceId))
                    .followRedirects(false).timeout(20000).get();
            String raceName = getNodesFromDoc(doc, AppConstant.RACE_NAME,"b",0).get(0).toString();
            String distance = getNodesFromDoc(doc, AppConstant.RACE_INFORMATION, AppConstant.SPAN, 0).get(1).toString().replace("m", "").trim();
            String startTime = getNodesFromDoc(doc, AppConstant.RACE_INFORMATION, AppConstant.SPAN, 1).get(1).toString();
            Elements raceInfor = doc.select(AppConstant.LINK+"["+AppConstant.HREF+"=" + raceId + "]");
            String raceNumber = raceInfor.select(AppConstant.RACE_NUMBER).get(0).childNodes().get(0).toString().replace("\n", "");
            String reslust = raceInfor.select(AppConstant.RACE_RESULT).isEmpty() ? "" : raceInfor.select(AppConstant.RACE_RESULT).get(0).childNodes().get(0).toString().replace("\n", "");
            Element sectionClass =  doc.select(AppConstant.SECTION_CLASS).get(0);
            Element bodyElement = sectionClass.getElementsByTag(AppConstant.BODY).get(0);
            Elements entrant = bodyElement.select(AppConstant.SILKCOLUMN_QUERY);
            topSportRaceDto.setId(raceId);
            topSportRaceDto.setRaceNumber(Integer.parseInt(raceNumber));
            topSportRaceDto.setRaceName(raceName);
            topSportRaceDto.setStartTime(ConvertBase.dateFormatFromString(startTime.trim()));
            topSportRaceDto.setDistance(Integer.parseInt(distance));
            topSportRaceDto.setResults(reslust);
            List<TopSportEntrantDto> topSportEntrantDtos = new ArrayList<>();
            for (int i = 0; i < entrant.size(); i++) {
                TopSportEntrantDto topSportEntrantDto = getEntrantById(entrant, i, raceId);
                topSportEntrantDtos.add(topSportEntrantDto);
            }
            topSportRaceDto.setRunners(topSportEntrantDtos);
            return topSportRaceDto;
        };

        Object result = crawUtils.crawlRace(crawlFunction, raceId, this.getClass().getName());

        if (result == null) {
            return null;
        }
        return (TopSportRaceDto) result;
    }

    public TopSportEntrantDto getEntrantById(Elements entrant, int i, String raceId) {
        TopSportEntrantDto topSportEntrantDto = new TopSportEntrantDto();
        Element element = entrant.get(i);
        String name = getNodesFromElement(element,AppConstant.NAME_CLASS);
        Integer number = Integer.valueOf(getNodesFromElement(element, AppConstant.SADDLE_CLASS).replace(".", "").trim());
        Integer barrier = null;
        if (!element.getElementsByClass(AppConstant.BARRIER_CLASS).isEmpty()) {
            barrier = Integer.valueOf(getNodesFromElement(element, AppConstant.BARRIER_CLASS).replaceAll(AppConstant.REPLACE_STRING, ""));
        }
        Boolean scratched = false;
        String scratchedTime = null;
        List<Float> listprice = new ArrayList<>();
        if (element.select(AppConstant.SCRATCHED_QUERY).isEmpty()) {
            float win = element.select(AppConstant.FIXED_WIN_QUERY).isEmpty() ? 0 : getPriceFromElement(element,AppConstant.FIXED_WIN_QUERY);
            float plex = element.select(AppConstant.FIXED_PLACE_QUERY).isEmpty() ? 0 : getPriceFromElement(element,AppConstant.FIXED_PLACE_QUERY);
            listprice.add(win);
            listprice.add(plex);
        } else {
            scratched = true;
            scratchedTime = getNodesFromElement(element,AppConstant.SCRATCHPAY_CLASS);

        }
        topSportEntrantDto.setRaceUUID(raceId);
        topSportEntrantDto.setEntrantName(name);
        topSportEntrantDto.setNumber(number);
        topSportEntrantDto.setBarrier(barrier);
        topSportEntrantDto.setScratched(scratched);
        topSportEntrantDto.setScratchedTime(scratchedTime == null ? null : ConvertBase.scratchedTimeFormatFromString(scratchedTime.replace(AppConstant.SCRATCHED_REPLACE, "").trim()));
        topSportEntrantDto.setPrice(listprice);
        return topSportEntrantDto;
    }

    public List<TopSportMeetingDto> getListMeetingByElement(Element element, LocalDate date) {
        List<TopSportMeetingDto> topSportMeetingDtos = new ArrayList<>();
        Elements elements = element.getElementsByTag(AppConstant.ROW);
        String raceType = element.parentNode() != null ? element.parentNode().attributes().get(AppConstant.CLASS).split(" ")[1] : null;
        elements.forEach(r -> {
            TopSportMeetingDto meeting = getMeetingByElement(r,date,raceType);
            topSportMeetingDtos.add(meeting);
        });
        return topSportMeetingDtos;
    }
    public TopSportMeetingDto getMeetingByElement( Element element, LocalDate date, String raceType) {
        TopSportMeetingDto meeting = new TopSportMeetingDto();
        Elements elements = element.getElementsByTag(AppConstant.COLUMN);
        meeting.setRaceType(raceType);
        meeting.setAdvertisedDate(ConvertBase.dateFormat(date));
        Element elementTag = element.getElementsByTag(AppConstant.SPAN).get(0);
        String meetingName = elementTag.childNodes().get(0).toString();
        String elmsVal = elements.get(0).attributes().get(AppConstant.CLASS);
        String countryCode = elmsVal.split(" ")[0].replace(AppConstant.RACEREGION, "");
        String stateCode = elmsVal.split(" ")[1].replace(AppConstant.RACESTATE, "");
        meeting.setName(meetingName);
        meeting.setCountry(countryCode);
        meeting.setState(stateCode);
        meeting.setId(meetingName + "_" + countryCode + "_" + stateCode);
        Elements elmRow = element.getElementsByTag(AppConstant.CELL);
        for (int j = 0; j < elmRow.size(); j++) {
            Elements raceIds = elmRow.select(AppConstant.LINK+"["+AppConstant.HREF+"]");
            List<String> listRaceId = new ArrayList<>();
            for (int i = 0; i < raceIds.size(); i++) {
                Element elementRace =raceIds.get(i);
                Element tagElement = elementRace.getElementsByTag(AppConstant.LINK).get(0);
                String raceId = tagElement.attributes().get(AppConstant.HREF);
                listRaceId.add(raceId);
            }
            meeting.setRaceId(listRaceId);
        }
        return meeting;
    }
    private List<Node> getNodesFromDoc(Document document, String className, String tag,int index){
        Element element = document.getElementsByClass(className).get(0);
        Element childElement = element.getElementsByTag(tag).get(index);
        return childElement.childNodes();
    }
    private String getNodesFromElement(Element element, String className){
        Element childElement = element.getElementsByClass(className).get(0);
        List<Node> childNodes = childElement.childNodes();
        return childNodes.get(0).toString();
    }
    private Float getPriceFromElement(Element element, String cssQuery){
        Element childElement = element.select(cssQuery).get(0);
        Element attributeElm = childElement.getElementsByAttribute(AppConstant.DATA_PRICE).get(0);
        Node node = attributeElm.childNodes().get(0);
        return Float.parseFloat(node.toString());
    }
}
