package com.tvf.clb.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tvf.clb.base.dto.RaceDto;
import com.tvf.clb.base.dto.SiteEnum;
import com.tvf.clb.base.exception.ApiRequestFailedException;
import com.tvf.clb.base.model.zbet.ZBetMeetingResponseRawData;
import com.tvf.clb.base.model.zbet.ZbetRaceResponseRawData;
import com.tvf.clb.base.utils.AppConstant;
import com.tvf.clb.base.utils.InstantDeserializer;
import com.tvf.clb.service.service.CrawUtils;
import com.tvf.clb.service.service.ZBetCrawlService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.util.CollectionUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.*;

class ZBetCrawlServiceTest {

    @Mock
    private CrawUtils crawUtils;

    @Mock
    private WebClient zbetWebClient;

    @InjectMocks
    private ZBetCrawlService serviceToTest;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        objectMapper = objectMapper();
    }

    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        JavaTimeModule module = new JavaTimeModule();
        module.addDeserializer(Instant.class, new InstantDeserializer());
        mapper.registerModule(module);
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        return mapper;
    }

    @AfterEach
    void tearDown() {
    }

    ZBetMeetingResponseRawData getDataCrawlMeeting() throws IOException {
        return objectMapper.readValue(new File("src/test/resources/zbet/ZbetMeetingData.json"), ZBetMeetingResponseRawData.class);
    }

    ZbetRaceResponseRawData getDataCrawlRace(String fileURL) throws IOException {
        return objectMapper.readValue(new File(fileURL), ZbetRaceResponseRawData.class);
    }

    /*
     * Function: GetTodayMeetings
     *
     * */

    @Test
    void giveNullLocalDate_whenGetTodayMeetings_thenThrowNullPointerException() {
        assertThrows(
                NullPointerException.class,
                () -> assertNull(serviceToTest.getTodayMeetings(null)),
                "LocalDate can't be null"
        );
    }

    @Test
    void giveErrorCallAPI_whenGetTodayMeetings_thenThrowApiRequestFailedException() {
        LocalDate date = LocalDate.now();
        String meetingQueryURI = AppConstant.ZBET_MEETING_QUERY.replace(AppConstant.DATE_PARAM, date.toString());
        String className = serviceToTest.getClass().getName();

        Mono<ZBetMeetingResponseRawData> resultMono = Mono.error(new ApiRequestFailedException());

        when(crawUtils.crawlData(zbetWebClient, meetingQueryURI, ZBetMeetingResponseRawData.class, className, 20L)).thenReturn(resultMono);

        StepVerifier.create(serviceToTest.getTodayMeetings(date))
                .expectErrorMatches(throwable -> throwable instanceof ApiRequestFailedException)
                .verify();

        verify(crawUtils, times(1)).saveFailedCrawlMeeting(className, date);
    }

    @Test
    void giveNotNullLocalDate_whenGetTodayMeetings_thenReturnFluxMeetingDto() throws IOException {

        LocalDate date = LocalDate.now();
        String meetingQueryURI = AppConstant.ZBET_MEETING_QUERY.replace(AppConstant.DATE_PARAM, date.toString());

        when(crawUtils.crawlData(zbetWebClient, meetingQueryURI, ZBetMeetingResponseRawData.class, serviceToTest.getClass().getName(), 20L))
                .thenReturn(Mono.just(getDataCrawlMeeting()));

        when(crawUtils.saveMeetingSiteAndRaceSite(anyMap(), any())).thenReturn(mock(Mono.class));

        StepVerifier.create(serviceToTest.getTodayMeetings(date).collectList())
                .expectNextMatches(meetingDtos -> meetingDtos.stream().allMatch(meetingDto -> meetingDto.getId() != null && meetingDto.getRaces().size() > 0
                        && meetingDto.getAdvertisedDate() != null))
                .verifyComplete();

        verify(crawUtils, never()).saveFailedCrawlMeeting(serviceToTest.getClass().getName(), date);
    }

    /*
     * Function: GetEntrantByRaceUUID
     *
     * */

    @Test
    void giveNullRaceId_whenGetEntrantByRaceUUID_thenThrowNullPointerException() {
        assertThrows(
                NullPointerException.class,
                () -> assertNull(serviceToTest.getEntrantByRaceUUID(null).block()),
                "RaceId can't be null"
        );
    }

    @Test
    void giveErrorCallAPI_whenGetEntrantByRaceUUID_thenReturnsMonoEmpty() {
        String raceId = UUID.randomUUID().toString();
        Mono<ZbetRaceResponseRawData> raceDto = Mono.error(new ApiRequestFailedException());

        String raceQueryURI = AppConstant.ZBET_RACE_QUERY.replace(AppConstant.ID_PARAM, raceId);
        when(crawUtils.crawlData(zbetWebClient, raceQueryURI, ZbetRaceResponseRawData.class, serviceToTest.getClass().getName(), 0L))
                .thenReturn(raceDto);

        StepVerifier.create(serviceToTest.getEntrantByRaceUUID(raceId))
                .expectNextCount(0)
                .verifyComplete();
    }

    @Test
    void giveStatusFinal_whenGetEntrantByRaceUUID_thenReturnMonoCrawlRaceData() throws IOException {
        String raceId = UUID.randomUUID().toString();
        Mono<ZbetRaceResponseRawData> raceDto = Mono.just(getDataCrawlRace("src/test/resources/zbet/ZbetRaceData.json"));

        String raceQueryURI = AppConstant.ZBET_RACE_QUERY.replace(AppConstant.ID_PARAM, raceId);
        when(crawUtils.crawlData(zbetWebClient, raceQueryURI, ZbetRaceResponseRawData.class, serviceToTest.getClass().getName(), 0L))
                .thenReturn(raceDto);

        StepVerifier.create(serviceToTest.getEntrantByRaceUUID(raceId))
                .expectNextMatches(crawlRaceData -> SiteEnum.ZBET.getId() == crawlRaceData.getSiteEnum().getId() &&
                        CollectionUtils.isEmpty(crawlRaceData.getInterimResult()) &&
                        crawlRaceData.getActualStart() == null &&
                        crawlRaceData.getAdvertisedStart() == null &&
                        !CollectionUtils.isEmpty(crawlRaceData.getMapEntrants()) &&
                        crawlRaceData.getFinalResult().get(SiteEnum.ZBET.getId()).equals("8,7,3,1")
                )
                .verifyComplete();
    }


    @Test
    void giveStatusInterim_whenGetEntrantByRaceUUID_thenReturnMonoCrawlRaceData() throws IOException {
        String raceId = UUID.randomUUID().toString();
        Mono<ZbetRaceResponseRawData> raceDto = Mono.just(getDataCrawlRace("src/test/resources/zbet/ZbetRaceData_TestStatusInterim.json"));

        String raceQueryURI = AppConstant.ZBET_RACE_QUERY.replace(AppConstant.ID_PARAM, raceId);
        when(crawUtils.crawlData(zbetWebClient, raceQueryURI, ZbetRaceResponseRawData.class, serviceToTest.getClass().getName(), 0L))
                .thenReturn(raceDto);

        StepVerifier.create(serviceToTest.getEntrantByRaceUUID(raceId))
                .expectNextMatches(crawlRaceData -> SiteEnum.ZBET.getId() == crawlRaceData.getSiteEnum().getId() &&
                        CollectionUtils.isEmpty(crawlRaceData.getFinalResult()) &&
                        crawlRaceData.getActualStart() == null &&
                        crawlRaceData.getAdvertisedStart() == null &&
                        !CollectionUtils.isEmpty(crawlRaceData.getMapEntrants()) &&
                        crawlRaceData.getInterimResult().get(SiteEnum.ZBET.getId()).equals("1,6,7,3")
                )
                .verifyComplete();
    }


    /*
     * Function: CrawlAndSaveEntrantsInRace
     *
     * */

    @Test
    void giveNullRaceDto_whenCrawlAndSaveEntrantsInRace_thenThrowsNullPointerException() {
        assertThrows(
                NullPointerException.class,
                () -> serviceToTest.crawlAndSaveEntrantsInRace(null, LocalDate.now()).subscribe(),
                "RaceDto can't be null"
        );
    }


    @Test
    void giveErrorCallAPI_whenCrawlAndSaveEntrantsInRace_thenThrowApiRequestFailedException() {
        RaceDto raceDto = new RaceDto();
        raceDto.setId(UUID.randomUUID().toString());
        Mono<ZbetRaceResponseRawData> apiResponseMono = Mono.error(new ApiRequestFailedException());

        String raceQueryURI = AppConstant.ZBET_RACE_QUERY.replace(AppConstant.ID_PARAM, raceDto.getId());
        when(crawUtils.crawlData(zbetWebClient, raceQueryURI, ZbetRaceResponseRawData.class, serviceToTest.getClass().getName(), 0L))
                .thenReturn(apiResponseMono);

        StepVerifier.create(serviceToTest.crawlAndSaveEntrantsInRace(raceDto, LocalDate.now()))
                .expectErrorMatches(throwable -> throwable instanceof ApiRequestFailedException)
                .verify();

        verify(crawUtils, times(1)).saveFailedCrawlRace(serviceToTest.getClass().getName(), raceDto, LocalDate.now());
    }


    @Test
    void giveNotNullRaceId_whenCrawlAndSaveEntrantsInRace_thenReturnFluxEntrantDto() throws IOException {

        RaceDto raceDto = new RaceDto();
        raceDto.setId(UUID.randomUUID().toString());
        raceDto.setStatus(AppConstant.STATUS_FINAL);
        raceDto.setRaceId(1L);
        Mono<ZbetRaceResponseRawData> apiResponseMono = Mono.just(getDataCrawlRace("src/test/resources/zbet/ZbetRaceData.json"));

        String raceQueryURI = AppConstant.ZBET_RACE_QUERY.replace(AppConstant.ID_PARAM, raceDto.getId());
        when(crawUtils.crawlData(zbetWebClient, raceQueryURI, ZbetRaceResponseRawData.class, serviceToTest.getClass().getName(), 0L))
                .thenReturn(apiResponseMono);
        when(crawUtils.getIdForNewRaceAndSaveRaceSite(eq(raceDto), anyList(), anyInt())).thenReturn(Mono.empty());

        StepVerifier.create(serviceToTest.crawlAndSaveEntrantsInRace(raceDto, LocalDate.now()).collectList())
                .expectNextMatches(entrantDtos -> entrantDtos.stream().allMatch(entrantDto -> entrantDto.getId() != null
                        && entrantDto.getName() != null
                        && entrantDto.getNumber() != null
                        && !CollectionUtils.isEmpty(entrantDto.getCurrentSitePrice()))
                )
                .verifyComplete();

        verify(crawUtils, never()).saveFailedCrawlRace(serviceToTest.getClass().getName(), raceDto, LocalDate.now());

        verify(crawUtils, times(1)).saveEntrantCrawlDataToRedis(any(), any(), any());
        verify(crawUtils, times(1)).saveEntrantsPriceIntoDB(any(), any(), any());

    }


    @Test
    void giveNullPrice_whenCrawlAndSaveEntrantsInRace_thenReturnFluxEntrantDto() throws IOException {

        RaceDto raceDto = new RaceDto();
        raceDto.setId(UUID.randomUUID().toString());
        raceDto.setStatus(AppConstant.STATUS_FINAL);
        raceDto.setRaceId(1L);
        Mono<ZbetRaceResponseRawData> apiResponseMonoTestPrice = Mono.just(getDataCrawlRace("src/test/resources/zbet/ZbetRaceData_TestPriceNull.json"));

        String raceQueryURIPriceNull = AppConstant.ZBET_RACE_QUERY.replace(AppConstant.ID_PARAM, raceDto.getId());
        when(crawUtils.crawlData(zbetWebClient, raceQueryURIPriceNull, ZbetRaceResponseRawData.class, serviceToTest.getClass().getName(), 0L))
                .thenReturn(apiResponseMonoTestPrice);
        when(crawUtils.getIdForNewRaceAndSaveRaceSite(eq(raceDto), anyList(), anyInt())).thenReturn(Mono.empty());

        StepVerifier.create(serviceToTest.crawlAndSaveEntrantsInRace(raceDto, LocalDate.now()).collectList())
                .expectNextMatches(entrantDtos -> entrantDtos.stream().allMatch(entrantDto -> CollectionUtils.isEmpty(entrantDto.getCurrentSitePrice()))
                )
                .verifyComplete();

        verify(crawUtils, never()).saveFailedCrawlRace(serviceToTest.getClass().getName(), raceDto, LocalDate.now());

        verify(crawUtils, times(1)).saveEntrantCrawlDataToRedis(any(), any(), any());
        verify(crawUtils, times(1)).saveEntrantsPriceIntoDB(any(), any(), any());

    }


    @Test
    void giveCantMapProductCode_whenCrawlAndSaveEntrantsInRace_thenReturnFluxEntrantDto() throws IOException {

        RaceDto raceDto = new RaceDto();
        raceDto.setId(UUID.randomUUID().toString());
        raceDto.setStatus(AppConstant.STATUS_FINAL);
        raceDto.setRaceId(1L);
        Mono<ZbetRaceResponseRawData> apiResponseMono = Mono.just(getDataCrawlRace("src/test/resources/zbet/ZbetRaceData_TestCantMapPriceCode.json"));

        String raceQueryURI = AppConstant.ZBET_RACE_QUERY.replace(AppConstant.ID_PARAM, raceDto.getId());
        when(crawUtils.crawlData(zbetWebClient, raceQueryURI, ZbetRaceResponseRawData.class, serviceToTest.getClass().getName(), 0L))
                .thenReturn(apiResponseMono);
        when(crawUtils.getIdForNewRaceAndSaveRaceSite(eq(raceDto), anyList(), anyInt())).thenReturn(Mono.empty());

        StepVerifier.create(serviceToTest.crawlAndSaveEntrantsInRace(raceDto, LocalDate.now()).collectList())
                .expectNextMatches(entrantDtos -> entrantDtos.stream().allMatch(entrantDto -> CollectionUtils.isEmpty(entrantDto.getCurrentSitePrice()))
                )
                .verifyComplete();

        verify(crawUtils, never()).saveFailedCrawlRace(serviceToTest.getClass().getName(), raceDto, LocalDate.now());

        verify(crawUtils, times(1)).saveEntrantCrawlDataToRedis(any(), any(), any());
        verify(crawUtils, times(1)).saveEntrantsPriceIntoDB(any(), any(), any());

    }



}
