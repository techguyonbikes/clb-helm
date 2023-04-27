package com.tvf.clb.service.service;

import com.tvf.clb.base.anotation.ClbService;
import com.tvf.clb.base.dto.*;
import com.tvf.clb.base.dto.topsport.TopSportEntrantDto;
import com.tvf.clb.base.dto.topsport.TopSportMeetingDto;
import com.tvf.clb.base.dto.topsport.TopSportRaceDto;
import com.tvf.clb.base.entity.Entrant;
import com.tvf.clb.base.entity.Meeting;
import com.tvf.clb.base.entity.Race;
import com.tvf.clb.base.exception.ApiRequestFailedException;
import com.tvf.clb.base.model.CrawlEntrantData;
import com.tvf.clb.base.model.CrawlRaceData;
import com.tvf.clb.base.utils.AppConstant;
import com.tvf.clb.base.utils.ConvertBase;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Node;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Flux;
import java.io.IOException;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.print.Doc;


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
        log.info("Start getting the API from TOPSPORT.");
        Document doc = null;
        try {
            doc = Jsoup.connect(AppConstant.TOPSPORT_MEETING_QUERY.replace(AppConstant.DATE_PARAM, ConvertBase.getDateOfWeek(date)))
                    .followRedirects(false).get();
            Elements links = doc.getElementsByTag(AppConstant.BODY);
            List<TopSportMeetingDto> topSportMeetingDtos = new ArrayList<>();
            for (Element link : links) {
                topSportMeetingDtos.addAll(getListMeetingByElement(link, date));
            }
            getAllAusMeetings(topSportMeetingDtos, date);
        } catch (IOException e) {
            log.error(e.getMessage());
        }
        return null;
    }

    private List<MeetingDto> getAllAusMeetings(List<TopSportMeetingDto> meetingDtoList, LocalDate date) {
        List<TopSportMeetingDto> newListMeeting = meetingDtoList.stream().filter(r -> AppConstant.VALID_CHECK_CODE_STATE_DIFF.contains(r.getState())).collect(Collectors.toList());
        List<MeetingDto> listMeetingDto = newListMeeting.stream().map(MeetingMapper:: toMeetingDtoFromTOP).collect(Collectors.toList());
        saveMeeting(newListMeeting);
        newListMeeting.forEach(meeting -> {
            try {
                List<TopSportRaceDto> topSportRaceDtoList = getAllRaceByTOP(meeting);
                saveRace(topSportRaceDtoList);
                topSportRaceDtoList.forEach(race -> crawlAndSaveEntrantsInRace(race, date));

            } catch (IOException e) {
                log.error(e.getMessage());
            }
        });
        return listMeetingDto;
    }

    public void saveMeeting(List<TopSportMeetingDto> meetingRawData) {
        List<Meeting> newMeetings = meetingRawData.stream().map(MeetingMapper::toMeetingEntityFromTOP).collect(Collectors.toList());
        crawUtils.saveMeetingSite(newMeetings, AppConstant.TOPSPORT_SITE_ID);
    }

    public void saveRace(List<TopSportRaceDto> raceDtoList) {
        List<Race> newRaces = raceDtoList.stream().map(MeetingMapper::toRaceEntityFromTOP).collect(Collectors.toList());
        crawUtils.saveRaceSite(newRaces, AppConstant.TOPSPORT_SITE_ID);
    }

    @Override
    public CrawlRaceData getEntrantByRaceUUID(String raceId) {
        try {
            TopSportRaceDto topSportRaceDto = getRacesByTOP(raceId);
            List<TopSportEntrantDto> allEntrant = topSportRaceDto.getRunners();
            Map<Integer, CrawlEntrantData> entrantMap = new HashMap<>();
            allEntrant.forEach(x -> {
                Map<Integer, List<Float>> priceFluctuations = new HashMap<>();
                priceFluctuations.put(AppConstant.TOPSPORT_SITE_ID, x.getPrice());
                entrantMap.put(x.getBarrier(), new CrawlEntrantData(0, priceFluctuations));
            });
            CrawlRaceData result = new CrawlRaceData();
            result.setSiteId(SiteEnum.TOP_SPORT.getId());
            result.setMapEntrants(entrantMap);
            return result;
        } catch (IOException e) {
            throw new ApiRequestFailedException("API request failed: " + e.getMessage(), e);
        }
    }

    public void crawlAndSaveEntrantsInRace(TopSportRaceDto topSportRaceDto, LocalDate date) {
        List<TopSportEntrantDto> allEntrant = topSportRaceDto.getRunners();
        RaceDto raceDto = RaceResponseMapper.toRaceDTO(topSportRaceDto);
        saveEntrants(allEntrant, String.format("%s - %s - %s - %s", raceDto.getMeetingName(), raceDto.getNumber(),
                raceDto.getRaceType(), date), raceDto);
    }

    public void saveEntrants(List<TopSportEntrantDto> entrantRawData, String raceName, RaceDto raceDto) {
        List<Entrant> newEntrants = entrantRawData.stream().distinct().map(EntrantMapper::toEntrantEntity).collect(Collectors.toList());
        crawUtils.saveEntrantCrawlDataToRedis(newEntrants, AppConstant.TOPSPORT_SITE_ID, raceName, raceDto);
    }

    public List<TopSportRaceDto> getAllRaceByTOP(TopSportMeetingDto meetingDto) throws IOException {
        List<TopSportRaceDto> list = new ArrayList<>();
        for (int i = 0; i < meetingDto.getRaceId().size(); i++) {
            try {
                TopSportRaceDto topSportRaceDto = getRacesByTOP(meetingDto.getRaceId().get(i));
                topSportRaceDto.setRaceType(ConvertBase.convertRaceTypeOfTOP(meetingDto.getRaceType()));
                topSportRaceDto.setMeetingName(meetingDto.getName().toUpperCase());
                list.add(topSportRaceDto);
            } catch (IOException e) {
                log.error(e.getMessage());
            }
        }
        return list;
    }

    public TopSportRaceDto getRacesByTOP(String raceId) throws IOException {
        try {
            TopSportRaceDto topSportRaceDto = new TopSportRaceDto();
            Document doc = Jsoup.connect(AppConstant.TOPSPORT_RACE_QUERY.replace(AppConstant.ID_PARAM, raceId))
                    .followRedirects(false).timeout(2000).get();
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
        } catch (IOException e) {
            throw new IOException(e.getMessage());
        }
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
        List<Node> childNodes = childElement.childNodes();
        return childNodes;
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
