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
import com.tvf.clb.base.utils.CommonUtils;
import com.tvf.clb.base.utils.ConvertBase;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;


@ClbService(componentType = AppConstant.TOP_SPORT)
@Slf4j
@AllArgsConstructor
public class TopSportCrawlService implements ICrawlService {
    private final CrawUtils crawUtils;
    private final WebClient topSportWebClient;

    @Override
    public Flux<MeetingDto> getTodayMeetings(LocalDate date) {
        log.info("Start getting the API from TopSport.");
        String className = this.getClass().getName();
        String uri = AppConstant.TOPSPORT_MEETING_QUERY.replace(AppConstant.DATE_PARAM, ConvertBase.getDateOfWeek(date));
        return crawUtils.crawlData(topSportWebClient, uri, Document.class, className, 5L)
                .doOnError(throwable -> crawUtils.saveFailedCrawlMeeting(className, date))
                .flatMapIterable(doc -> {
                    Elements links = doc.getElementsByTag(AppConstant.BODY);
                    List<TopSportMeetingDto> topSportMeetingDtos = new ArrayList<>();
                    for (Element link : links) {
                        topSportMeetingDtos.addAll(getListMeetingByElement(link, date));
                    }
                    return getAllAusMeetings(topSportMeetingDtos, date);
                });
    }

    private List<MeetingDto> getAllAusMeetings(List<TopSportMeetingDto> meetingDtoList, LocalDate date) {
        List<MeetingDto> listMeetingDto = meetingDtoList.stream().map(MeetingMapper::toMeetingDtoFromTOP).collect(Collectors.toList());
        Map<Meeting, List<Race>> mapMeetingAndRace = new HashMap<>();
        Map<TopSportMeetingDto, List<String>> map = new HashMap<>();
        meetingDtoList.forEach(meetingDto -> map.put(meetingDto, meetingDto.getRaceId()));
        Flux<List<TopSportRaceDto>> flux = Flux.empty();
        for (Map.Entry<TopSportMeetingDto, List<String>> entry : map.entrySet()) {
            TopSportMeetingDto meetingDto = entry.getKey();
            List<String> listRaceUUID = entry.getValue();
            List<Race> racesInMeeting = new ArrayList<>();
            flux = flux.concatWith(Flux.fromIterable(listRaceUUID)
                    .flatMap(uuid -> crawlAndSaveEntrantsAndRace(meetingDto, uuid, date))
                    .doOnNext(topSportRaceDto -> racesInMeeting.add(MeetingMapper.toRaceEntityFromTOP(topSportRaceDto)))
                    .collectList()
                    .doOnNext(list -> mapMeetingAndRace.put(MeetingMapper.toMeetingEntityFromTOP(meetingDto), racesInMeeting)));
        }
        flux.collectList()
                .flatMap(m -> crawUtils.saveMeetingSiteAndRaceSite(mapMeetingAndRace, SiteEnum.TOP_SPORT.getId()))
                .subscribe();

        return listMeetingDto;
    }


    @Override
    public Mono<CrawlRaceData> getEntrantByRaceUUID(String raceId) {
        Mono<TopSportRaceDto> topSportRaceApiResponseMono = getRacesByTOP(raceId);
        return topSportRaceApiResponseMono.onErrorResume(throwable -> Mono.empty())
                .filter(apiResponse -> apiResponse.getRunners() != null)
                .map(topSportRaceDto -> {
                    List<TopSportEntrantDto> allEntrant = topSportRaceDto.getRunners();
                    Map<Integer, CrawlEntrantData> entrantMap = new HashMap<>();
                    allEntrant.forEach(x -> {
                        Map<Integer, List<Float>> priceFluctuations = new HashMap<>();
                        priceFluctuations.put(AppConstant.TOPSPORT_SITE_ID, x.getPriceWins());

                        Map<Integer, List<Float>> pricePlaces = new HashMap<>();
                        pricePlaces.put(AppConstant.TOPSPORT_SITE_ID, x.getPricePlaces());

                        Map<Integer, Float> priceScratchPlaces = new HashMap<>();
                        if (x.getPricePlacesScratch() != null) {
                            priceScratchPlaces.put(AppConstant.TOPSPORT_SITE_ID, x.getPricePlacesScratch());
                        }

                        Map<Integer, Float> priceScratchWin = new HashMap<>();
                        if (x.getPriceWinScratch() != null) {
                            priceScratchWin.put(AppConstant.TOPSPORT_SITE_ID, x.getPriceWinScratch());
                        }

                        entrantMap.put(x.getNumber(), new CrawlEntrantData(0, priceFluctuations, pricePlaces, priceScratchWin, priceScratchPlaces));
                    });
                    CrawlRaceData result = new CrawlRaceData();
                    result.setSiteEnum(SiteEnum.TOP_SPORT);
                    result.setMapEntrants(entrantMap);
                    if (!topSportRaceDto.getResults().isEmpty()) {
                        result.setFinalResult(Collections.singletonMap(AppConstant.TOPSPORT_SITE_ID, topSportRaceDto.getResults()));
                    }
                    return result;
                });
    }

    private Mono<TopSportRaceDto> crawlAndSaveEntrantsAndRace(TopSportMeetingDto meetingDto, String raceSiteUUID, LocalDate date) {
        Mono<TopSportRaceDto> topSportRaceCrawlData = getRacesByTOP(raceSiteUUID);
        return topSportRaceCrawlData.doOnError(throwable -> crawUtils.saveFailedCrawlRaceForTopSport(meetingDto, raceSiteUUID, date))
                .filter(raceRawData -> raceRawData.getRaceName() != null || raceRawData.getRunners() != null || raceRawData.getMeetingName() != null)
                .flatMap(topSportRaceDto -> {
                    topSportRaceDto.setRaceType(ConvertBase.convertRaceTypeOfTOP(meetingDto.getRaceType()));
                    topSportRaceDto.setMeetingName(meetingDto.getName().toUpperCase());

                    List<TopSportEntrantDto> allEntrantRawData = topSportRaceDto.getRunners();
                    RaceDto raceDto = RaceResponseMapper.toRaceDTO(topSportRaceDto);

                    List<Entrant> newEntrants = allEntrantRawData.stream().distinct().map(EntrantMapper::toEntrantEntity).collect(Collectors.toList());

                    Mono<Long> raceIdMono = crawUtils.getIdForNewRace(raceDto, newEntrants);

                    return raceIdMono.map(raceId -> {
                        raceDto.setRaceId(raceId);
                        if (!topSportRaceDto.getResults().isEmpty()) {
                            crawUtils.updateRaceFinalResultIntoDB(raceDto.getRaceId(), topSportRaceDto.getResults(), SiteEnum.TOP_SPORT.getId());
                            raceDto.setFinalResult(topSportRaceDto.getResults());
                        }
                        saveEntrant(newEntrants, raceDto);
                        return topSportRaceDto;
                    });
                });
    }

    private void saveEntrant(List<Entrant> newEntrants, RaceDto raceDto) {
        crawUtils.saveEntrantCrawlDataToRedis(newEntrants, raceDto, SiteEnum.TOP_SPORT.getId());
        crawUtils.saveEntrantsPriceIntoDB(newEntrants, raceDto.getRaceId(), SiteEnum.TOP_SPORT.getId());
    }

    private Mono<TopSportRaceDto> getRacesByTOP(String raceId) {
        String className = this.getClass().getName();
        String uri = AppConstant.TOPSPORT_RACE_QUERY.replace(AppConstant.ID_PARAM, raceId);
        Mono<Document> documentMono = crawUtils.crawlData(topSportWebClient, uri, Document.class, className, 5L);
        return documentMono
                .onErrorResume(throwable -> Mono.empty())
                .map(doc -> {
                    TopSportRaceDto topSportRaceDto = new TopSportRaceDto();
                    String raceName = CommonUtils.applyIfNotEmpty(getNodesFromDoc(doc, AppConstant.RACE_NAME, "b", 0, "NAME"), functions ->  functions.get(0).toString().toUpperCase());

                    String distance = CommonUtils.applyIfNotEmpty(getNodesFromDoc(doc, AppConstant.RACE_INFORMATION, AppConstant.SPAN, 0, AppConstant.TAG_DISTANCE), functions ->  functions.get(1).toString().replace("m", "").trim());
                    String startTime = CommonUtils.applyIfNotEmpty(getNodesFromDoc(doc, AppConstant.RACE_INFORMATION, AppConstant.SPAN, 1, AppConstant.TAG_START_TIME), functions ->  functions.get(1).toString());
                    Elements raceElements = doc.select(AppConstant.LINK + "[" + AppConstant.HREF + "=" + raceId + "]");
                    String raceNumber = CommonUtils.applyIfNotEmpty(raceElements.select(AppConstant.RACE_NUMBER).get(0).childNodes(), functions ->  functions.get(0).toString().replace("\n", ""));
                    String result = raceElements.select(AppConstant.RACE_RESULT).isEmpty() ? "" : CommonUtils.applyIfNotEmpty(raceElements.select(AppConstant.RACE_RESULT).get(0).childNodes(), functions ->  functions.get(0).toString().replace("\n", ""));
                    Element sectionClass = CommonUtils.applyIfNotEmpty(doc.select(AppConstant.SECTION_CLASS), functions ->  functions.get(0));
                    Element bodyElement = CommonUtils.applyIfNotEmpty(sectionClass, functions ->  functions.getElementsByTag(AppConstant.BODY).get(0));
                    Elements entrant = CommonUtils.applyIfNotEmpty(bodyElement, functions ->  functions.select(AppConstant.SILKCOLUMN_QUERY));
                    topSportRaceDto.setId(raceId);
                    topSportRaceDto.setRaceNumber(raceNumber == null ? 0 : Integer.parseInt(raceNumber));
                    topSportRaceDto.setRaceName(raceName == null ? AppConstant.RACE_NAME_DEFAULT + raceNumber : raceName);
                    topSportRaceDto.setStartTime(ConvertBase.dateFormatFromString(startTime));
                    topSportRaceDto.setDistance(distance == null ? 0 : Integer.parseInt(distance));
                    topSportRaceDto.setResults(result);
                    List<TopSportEntrantDto> topSportEntrantDtos = new ArrayList<>();
                    for (int i = 0; i < Objects.requireNonNull(entrant, "[TOPSPORT] Crawl null entrant data with raceId: "+raceId).size(); i++) {
                        TopSportEntrantDto topSportEntrantDto = getEntrantById(entrant, i, raceId);
                        topSportEntrantDtos.add(topSportEntrantDto);
                    }
                    topSportRaceDto.setRunners(topSportEntrantDtos);
                    return topSportRaceDto;
                });
    }

    private TopSportEntrantDto getEntrantById(Elements entrant, int i, String raceId) {
        TopSportEntrantDto topSportEntrantDto = new TopSportEntrantDto();
        Element element = entrant.get(i);
        String name = getNodesFromElement(element, AppConstant.NAME_CLASS).toUpperCase();
        Integer number = Integer.valueOf(getNodesFromElement(element, AppConstant.SADDLE_CLASS).replace(".", "").trim());
        Integer barrier = null;
        if (!element.getElementsByClass(AppConstant.BARRIER_CLASS).isEmpty()) {
            barrier = Integer.valueOf(getNodesFromElement(element, AppConstant.BARRIER_CLASS).replaceAll(AppConstant.REPLACE_STRING, ""));
        }
        boolean scratched = false;
        String scratchedTime = null;
        Float priceWinScratched = null;
        Float pricePlaceScratched = null;

        List<Float> listPriceWin = new ArrayList<>();
        List<Float> listPricePlace = new ArrayList<>();
        if (element.select(AppConstant.SCRATCHED_QUERY).isEmpty()) {
            Float openPrice = getPriceFromElement(element.select(AppConstant.OPEN_PRICE_WIN), raceId, AppConstant.PRICE_WIN);
            Float win = getPriceFromElement(element.select(AppConstant.FIXED_WIN_QUERY), raceId, AppConstant.PRICE_WIN);
            Float place = getPriceFromElement(element.select(AppConstant.FIXED_PLACE_QUERY), raceId, AppConstant.PRICE_PLACE);
            Float flucInitPrice = getPriceFromElement(element.select(AppConstant.FLUC_INIT_PRICE), raceId, AppConstant.PRICE_WIN);
            if (openPrice != null) {
                listPriceWin.add(openPrice);
                if (win != null) {
                    listPriceWin.add(win);
                }
            } else if (flucInitPrice != null) {
                listPriceWin.add(flucInitPrice);
            }
            if (place != null) {
                listPricePlace.add(place);
            }
        } else {
            scratched = true;
            scratchedTime = getNodesFromElement(element, AppConstant.SCRATCHPAY_CLASS);
            priceWinScratched = getPriceFromElement(element.select(AppConstant.SCRATCH_PRICE_QUERY), raceId, AppConstant.PRICE_SCRATCH_WIN);
            pricePlaceScratched = getPriceFromElement(element.select(AppConstant.SCRATCH_PRICE_QUERY), raceId, AppConstant.PRICE_SCRATCH_PLACE);
        }
        topSportEntrantDto.setPricePlacesScratch(pricePlaceScratched);
        topSportEntrantDto.setPriceWinScratch(priceWinScratched);
        topSportEntrantDto.setRaceUUID(raceId);
        topSportEntrantDto.setEntrantName(name);
        topSportEntrantDto.setNumber(number);
        topSportEntrantDto.setBarrier(barrier);
        topSportEntrantDto.setScratched(scratched);
        topSportEntrantDto.setScratchedTime(scratchedTime == null ? null : ConvertBase.scratchedTimeFormatFromString(scratchedTime.replace(AppConstant.SCRATCHED_REPLACE, "").trim()));
        topSportEntrantDto.setPriceWins(listPriceWin);
        topSportEntrantDto.setPricePlaces(listPricePlace);
        return topSportEntrantDto;
    }

    private List<TopSportMeetingDto> getListMeetingByElement(Element element, LocalDate date) {
        List<TopSportMeetingDto> topSportMeetingDtos = new ArrayList<>();
        Elements elements = element.getElementsByTag(AppConstant.ROW);
        String raceType = element.parentNode() != null ? element.parentNode().attributes().get(AppConstant.CLASS).split(" ")[1] : null;
        elements.forEach(r -> {
            TopSportMeetingDto meeting = getMeetingByElement(r, date, raceType);
            topSportMeetingDtos.add(meeting);
        });
        return topSportMeetingDtos;
    }

    private TopSportMeetingDto getMeetingByElement(Element element, LocalDate date, String raceType) {
        TopSportMeetingDto meeting = new TopSportMeetingDto();
        Elements elements = element.getElementsByTag(AppConstant.COLUMN);
        meeting.setRaceType(raceType);
        meeting.setAdvertisedDate(ConvertBase.dateFormat(date));
        Element elementTag = CommonUtils.applyIfNotEmpty(element.getElementsByTag(AppConstant.SPAN), functions ->  functions.get(0));
        String meetingName = elementTag == null ? "" : CommonUtils.applyIfNotEmpty(elementTag.childNodes(), elementNode -> elementNode.get(0).toString().toUpperCase());
        String elmsVal = elements.get(0).attributes().get(AppConstant.CLASS);
        String countryCode = elmsVal.split(" ")[0].replace(AppConstant.RACEREGION, "");
        String stateCode = elmsVal.split(" ")[1].replace(AppConstant.RACESTATE, "");
        meeting.setName(meetingName);
        meeting.setCountry(countryCode);
        meeting.setState(stateCode);
        meeting.setId(meetingName + "_" + countryCode + "_" + stateCode);
        Elements elmRow = element.getElementsByTag(AppConstant.CELL);
        for (int j = 0; j < elmRow.size(); j++) {
            Elements raceIds = elmRow.select(AppConstant.LINK + "[" + AppConstant.HREF + "]");
            List<String> listRaceId = new ArrayList<>();
            for (Element elementRace : raceIds) {
                Element tagElement = elementRace.getElementsByTag(AppConstant.LINK).get(0);
                String raceId = tagElement.attributes().get(AppConstant.HREF);
                listRaceId.add(raceId);
            }
            meeting.setRaceId(listRaceId);
        }
        return meeting;
    }

    private List<Node> getNodesFromDoc(Document document, String className, String tag, int index, String property) {
        Element element = CommonUtils.applyIfNotEmpty(document.getElementsByClass(className), functions ->  functions.get(0));
        if (element == null) {
            return Collections.emptyList();
        }
        if (element.getElementsByTag(tag).size() > 1) {
            if (property.equals(AppConstant.TAG_DISTANCE) || property.equals(AppConstant.TAG_START_TIME)) {
                Element childElement = CommonUtils.applyIfNotEmpty(element.getElementsByTag(tag), functions ->  functions.get(index));
                return CommonUtils.applyIfNotEmpty(childElement, Node::childNodes);
            }
        } else {
            if (property.equals(AppConstant.TAG_DISTANCE)) {
                return Collections.emptyList();
            } else if (property.equals(AppConstant.TAG_START_TIME)) {
                Element childElement = CommonUtils.applyIfNotEmpty(element.getElementsByTag(tag), functions ->  functions.get(0));
                return CommonUtils.applyIfNotEmpty(childElement, Node::childNodes);
            }
        }
        Element childElement = CommonUtils.applyIfNotEmpty(element.getElementsByTag(tag), functions ->  functions.get(index));
        return CommonUtils.applyIfNotEmpty(childElement, Node::childNodes);
    }

    private String getNodesFromElement(Element element, String className) {
        Element childElement = CommonUtils.applyIfNotEmpty(element.getElementsByClass(className), functions ->  functions.get(0));
        List<Node> childNodes = CommonUtils.applyIfNotEmpty(childElement, Node::childNodes);
        return CommonUtils.applyIfNotEmpty(childNodes, functions ->  functions.get(0).toString());
    }

    private Float getPriceFromElement(Elements element, String raceId, String typePrice) {
        if (element.isEmpty()){
            return null;
        }
        Node node = null;
        try {
            Element childElement = CommonUtils.applyIfNotEmpty(element, functions ->  functions.get(0));
            if (childElement == null) {
                return null;
            }
            Element attributeElm = childElement.getElementsByAttribute(AppConstant.DATA_PRICE).first();
            if (attributeElm != null) {
                node = attributeElm.childNodes().get(0);
            } else {
                if (typePrice.equals(AppConstant.PRICE_SCRATCH_WIN) || typePrice.equals(AppConstant.PRICE_SCRATCH_PLACE)) {
                    return getDocumentPriceScratch(childElement, typePrice);
                } else {
                    node = CommonUtils.applyIfNotEmpty(childElement.childNodes(), functions ->  functions.get(0));
                }
            }
            return node == null ? null : Float.parseFloat(node.toString());
        } catch (NullPointerException | NumberFormatException | IndexOutOfBoundsException e) {
            log.error("[TopSport] Failed to parse: {} with raceId: {}", node, raceId);
            return null;
        }
    }

    private Float getDocumentPriceScratch(Element childElement, String typePrice){
        if (childElement.childNodes().size() < 2) {
            return null;
        }
        int priceIndex = typePrice.equals(AppConstant.PRICE_SCRATCH_WIN) ? 0 : 1;
        return CommonUtils.getDollarPriceFromString(childElement.childNodes().get(2).toString(), priceIndex);
    }
}
