package com.tvf.clb.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tvf.clb.base.dto.RaceDto;
import com.tvf.clb.base.dto.SiteEnum;
import com.tvf.clb.base.entity.Race;
import com.tvf.clb.base.exception.ApiRequestFailedException;
import com.tvf.clb.base.model.pointbet.PointBetMeetingRawData;
import com.tvf.clb.base.model.pointbet.PointBetRaceApiResponse;
import com.tvf.clb.base.utils.AppConstant;
import com.tvf.clb.base.utils.InstantDeserializer;
import com.tvf.clb.service.repository.MeetingRepository;
import com.tvf.clb.service.repository.RaceRepository;
import com.tvf.clb.service.service.CrawUtils;
import com.tvf.clb.service.service.PointBetCrawlService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.util.CollectionUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class PointBetCrawlServiceTest {

    @Mock
    private CrawUtils crawUtils;

    @Mock
    private MeetingRepository meetingRepository;

    @Mock
    private RaceRepository raceRepository;

    @Mock
    private WebClient pointBetWebClient;

    private ObjectMapper objectMapper;

    @InjectMocks
    private PointBetCrawlService serviceToTest;

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
        String meetingQueryURI = AppConstant.POINT_BET_MEETING_QUERY.replace(AppConstant.DATE_PARAM, date.toString());
        String className = serviceToTest.getClass().getName();

        Mono<PointBetMeetingRawData[]> resultMono = Mono.error(new ApiRequestFailedException());

        when(crawUtils.crawlData(pointBetWebClient, meetingQueryURI, PointBetMeetingRawData[].class, className, 20L)).thenReturn(resultMono);

        StepVerifier.create(serviceToTest.getTodayMeetings(date))
                .expectErrorMatches(throwable -> throwable instanceof ApiRequestFailedException)
                .verify();

        verify(crawUtils, times(1)).saveFailedCrawlMeeting(className, date);
    }

    @Test
    void giveNotNullLocalDate_whenGetTodayMeetings_thenReturnFluxMeetingDto() throws IOException {

        LocalDate date = LocalDate.now();
        String meetingQueryURI = AppConstant.POINT_BET_MEETING_QUERY.replace(AppConstant.DATE_PARAM, date.toString());

        when(crawUtils.crawlData(pointBetWebClient, meetingQueryURI, PointBetMeetingRawData[].class, serviceToTest.getClass().getName(), 20L))
                .thenReturn(Mono.just(getDataCrawlMeeting()));

        when(meetingRepository.getMeetingIdsByNameContainsAndRaceTypeAndAdvertisedDateFrom(any(), any(), any())).thenReturn(Flux.fromIterable(new ArrayList<>()));
        when(raceRepository.getRaceByMeetingIdInAndNumberAndAdvertisedStart(any(), any(), any())).thenReturn(Mono.just(Race.builder().id(1L).build()));
        when(crawUtils.getRaceSitesFromMeetingIdAndRaces(any(), any(), any())).thenReturn(Flux.empty());
        when(crawUtils.saveMeetingSite(any(), any(), any())).thenReturn(Flux.empty());
        when(crawUtils.saveRaceSite(any(), any())).thenReturn(Flux.empty());

        StepVerifier.create(serviceToTest.getTodayMeetings(date).collectList())
                .expectNextMatches(meetingDtos -> meetingDtos.stream().allMatch(meetingDto -> meetingDto.getId() != null && meetingDto.getRaces().size() > 0
                        && meetingDto.getAdvertisedDate() != null))
                .verifyComplete();

        verify(crawUtils, never()).saveFailedCrawlMeeting(serviceToTest.getClass().getName(), date);
    }

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
        Mono<PointBetRaceApiResponse> raceDto = Mono.error(new ApiRequestFailedException());

        String raceQueryURI = AppConstant.POINT_BET_RACE_QUERY.replace(AppConstant.ID_PARAM, raceId);
        when(crawUtils.crawlData(pointBetWebClient, raceQueryURI, PointBetRaceApiResponse.class, serviceToTest.getClass().getName(), 0L))
                .thenReturn(raceDto);

        StepVerifier.create(serviceToTest.getEntrantByRaceUUID(raceId))
                .expectNextCount(0)
                .verifyComplete();
    }

    @Test
    void giveNotNullRaceId_whenGetEntrantByRaceUUID_thenReturnMonoCrawlRaceData() throws IOException {
        String raceId = "38395453";
        Mono<PointBetRaceApiResponse> raceDto = Mono.just(getDataCrawlRace("src/test/resources/pointbet/pointbet-race-api-response.json"));

        String raceQueryURI = AppConstant.POINT_BET_RACE_QUERY.replace(AppConstant.ID_PARAM, raceId);
        when(crawUtils.crawlData(pointBetWebClient, raceQueryURI, PointBetRaceApiResponse.class, serviceToTest.getClass().getName(), 0L))
                .thenReturn(raceDto);

        StepVerifier.create(serviceToTest.getEntrantByRaceUUID(raceId))
                .expectNextMatches(crawlRaceData -> SiteEnum.POINT_BET.getId() == crawlRaceData.getSiteEnum().getId() &&
                        CollectionUtils.isEmpty(crawlRaceData.getInterimResult()) &&
                        crawlRaceData.getActualStart() == null &&
                        crawlRaceData.getAdvertisedStart() == null

                )
                .verifyComplete();
    }

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
        Mono<PointBetRaceApiResponse> apiResponseMono = Mono.error(new ApiRequestFailedException());

        String raceQueryURI = AppConstant.POINT_BET_RACE_QUERY.replace(AppConstant.ID_PARAM, raceDto.getId());
        when(crawUtils.crawlData(pointBetWebClient, raceQueryURI, PointBetRaceApiResponse.class, serviceToTest.getClass().getName(), 0L))
                .thenReturn(apiResponseMono);

        StepVerifier.create(serviceToTest.crawlAndSaveEntrantsInRace(raceDto, LocalDate.now()))
                .expectErrorMatches(throwable -> throwable instanceof ApiRequestFailedException)
                .verify();

        verify(crawUtils, times(1)).saveFailedCrawlRace(serviceToTest.getClass().getName(), raceDto, LocalDate.now());
    }

    @Test
    void giveSuccessCallApi_whenCrawlAndSaveEntrantsInRace_thenReturnFluxEntrantDto() throws IOException {
        RaceDto raceDto = new RaceDto();
        raceDto.setId("38395453");
        raceDto.setRaceId(1L);
        Mono<PointBetRaceApiResponse> apiResponseMono = Mono.just(getDataCrawlRace("src/test/resources/pointbet/pointbet-race-api-response.json"));

        String raceQueryURI = AppConstant.POINT_BET_RACE_QUERY.replace(AppConstant.ID_PARAM, raceDto.getId());
        when(crawUtils.crawlData(pointBetWebClient, raceQueryURI, PointBetRaceApiResponse.class, serviceToTest.getClass().getName(), 0L))
                .thenReturn(apiResponseMono);

        when(crawUtils.getIdForNewRaceAndSaveRaceSite(eq(raceDto), anyList(), anyInt())).thenReturn(Mono.empty());

        StepVerifier.create(serviceToTest.crawlAndSaveEntrantsInRace(raceDto, LocalDate.now()).collectList())
                .expectNextMatches(entrantCrawlData -> entrantCrawlData.stream().allMatch(entrant -> entrant.getCurrentSitePrice() != null && entrant.getId() != null)
                                                        && entrantCrawlData.size() == 8)
                .verifyComplete();

        verify(crawUtils, never()).saveFailedCrawlRace(serviceToTest.getClass().getName(), raceDto, LocalDate.now());
    }

    private PointBetMeetingRawData[] getDataCrawlMeeting() throws IOException {
        return objectMapper.readValue(new File("src/test/resources/pointbet/pointbet-meeting-api-response.json"), PointBetMeetingRawData[].class);
    }

    private PointBetRaceApiResponse getDataCrawlRace(String fileURL) throws IOException {
        return objectMapper.readValue(new File(fileURL), PointBetRaceApiResponse.class);
    }

}