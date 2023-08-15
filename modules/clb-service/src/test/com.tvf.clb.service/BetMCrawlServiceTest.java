package com.tvf.clb.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tvf.clb.base.dto.RaceDto;
import com.tvf.clb.base.dto.SiteEnum;
import com.tvf.clb.base.exception.ApiRequestFailedException;
import com.tvf.clb.base.model.betm.BetMMeetingApiResponse;
import com.tvf.clb.base.model.betm.BetMRaceApiResponse;
import com.tvf.clb.base.utils.AppConstant;
import com.tvf.clb.base.utils.ConvertBase;
import com.tvf.clb.base.utils.InstantDeserializer;
import com.tvf.clb.service.service.BetMCrawlService;
import com.tvf.clb.service.service.CrawUtils;
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
import static org.mockito.Mockito.*;

class BetMCrawlServiceTest {
    @Mock
    private CrawUtils crawUtils;

    @Mock
    private WebClient betMWebClient;

    @InjectMocks
    private BetMCrawlService serviceToTest;

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

    private BetMMeetingApiResponse crawlMeetingData() throws IOException {
        return objectMapper.readValue(new File("src/test/resources/betm/betm-meeting-api-response.json"), BetMMeetingApiResponse.class);
    }

    private BetMRaceApiResponse getDataCrawlRace() throws IOException {
        return objectMapper.readValue(new File("src/test/resources/betm/betm-race-api-response.json"), BetMRaceApiResponse.class);
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
        String meetingQueryURI = AppConstant.BET_M_MEETING_QUERY.replace(AppConstant.DATE_PARAM, ConvertBase.getDateOfWeek(date));
        String className = serviceToTest.getClass().getName();

        Mono<BetMMeetingApiResponse> resultMono = Mono.error(new ApiRequestFailedException());

        when(crawUtils.crawlData(betMWebClient, meetingQueryURI, BetMMeetingApiResponse.class, className, 5L)).thenReturn(resultMono);

        StepVerifier.create(serviceToTest.getTodayMeetings(date))
                .expectErrorMatches(throwable -> throwable instanceof ApiRequestFailedException)
                .verify();

        verify(crawUtils, times(1)).saveFailedCrawlMeeting(className, date);
    }

    @Test
    void giveNotNullLocalDate_whenGetTodayMeetings_thenReturnFluxMeetingDto() throws IOException {

        LocalDate date = LocalDate.now();
        String meetingQueryURI = AppConstant.BET_M_MEETING_QUERY.replace(AppConstant.DATE_PARAM, ConvertBase.getDateOfWeek(date));

        when(crawUtils.crawlData(betMWebClient, meetingQueryURI, BetMMeetingApiResponse.class, serviceToTest.getClass().getName(), 5L))
                .thenReturn(Mono.just(crawlMeetingData()));

        when(crawUtils.saveMeetingSiteAndRaceSite(anyMap(), any())).thenReturn(mock(Mono.class));

        StepVerifier.create(serviceToTest.getTodayMeetings(date).collectList())
                .expectNextMatches(meetingDtos -> meetingDtos.stream().allMatch(meetingDto -> meetingDto.getId() != null && meetingDto.getRaces().size() > 0
                        && meetingDto.getAdvertisedDate() == null))
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
        Mono<BetMRaceApiResponse> raceDto = Mono.error(new ApiRequestFailedException());

        String raceQueryURI = AppConstant.BET_M_RACE_QUERY.replace(AppConstant.ID_PARAM, raceId);
        when(crawUtils.crawlData(betMWebClient, raceQueryURI, BetMRaceApiResponse.class, serviceToTest.getClass().getName(), 5L))
                .thenReturn(raceDto);

        StepVerifier.create(serviceToTest.getEntrantByRaceUUID(raceId))
                .expectNextCount(0)
                .verifyComplete();
    }

    @Test
    void giveNotNullRaceId_whenGetEntrantByRaceUUID_thenReturnMonoCrawlRaceData() throws IOException {
        String raceId = UUID.randomUUID().toString();
        Mono<BetMRaceApiResponse> raceDto = Mono.just(getDataCrawlRace());

        String raceQueryURI = AppConstant.BET_M_RACE_QUERY.replace(AppConstant.ID_PARAM, raceId);
        when(crawUtils.crawlData(betMWebClient, raceQueryURI, BetMRaceApiResponse.class, serviceToTest.getClass().getName(), 5L))
                .thenReturn(raceDto);

        String top4Entrants = "1,2,4,6";

        StepVerifier.create(serviceToTest.getEntrantByRaceUUID(raceId))
                .expectNextMatches(crawlRaceData -> SiteEnum.BET_M.getId() == crawlRaceData.getSiteEnum().getId() &&
                        CollectionUtils.isEmpty(crawlRaceData.getInterimResult()) &&
                        crawlRaceData.getActualStart() == null &&
                        crawlRaceData.getAdvertisedStart() == null &&
                        !CollectionUtils.isEmpty(crawlRaceData.getMapEntrants()) &&
                        crawlRaceData.getFinalResult().get(SiteEnum.BET_M.getId()).equals(top4Entrants)
                )
                .verifyComplete();
    }

    @Test
    void giveErrorCallAPI_whenCrawlAndSaveEntrantsInRace_thenThrowApiRequestFailedException() {
        RaceDto raceDto = new RaceDto();
        raceDto.setId(UUID.randomUUID().toString());
        Mono<BetMRaceApiResponse> apiResponseMono = Mono.error(new ApiRequestFailedException());

        String raceQueryURI = AppConstant.BET_M_RACE_QUERY.replace(AppConstant.ID_PARAM, raceDto.getId());
        when(crawUtils.crawlData(betMWebClient, raceQueryURI, BetMRaceApiResponse.class, serviceToTest.getClass().getName(), 5L))
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
        raceDto.setRaceId(1L);
        Mono<BetMRaceApiResponse> apiResponseMono = Mono.just(getDataCrawlRace());

        String raceQueryURI = AppConstant.BET_M_RACE_QUERY.replace(AppConstant.ID_PARAM, raceDto.getId());
        when(crawUtils.crawlData(betMWebClient, raceQueryURI, BetMRaceApiResponse.class, serviceToTest.getClass().getName(), 5L))
                .thenReturn(apiResponseMono);
        when(crawUtils.getIdForNewRaceAndSaveRaceSite(eq(raceDto), anyList(), anyInt())).thenReturn(Mono.empty());

        StepVerifier.create(serviceToTest.crawlAndSaveEntrantsInRace(raceDto, LocalDate.now()).collectList())
                .expectNextMatches(entrantDtos -> entrantDtos.stream().allMatch(entrantDto -> entrantDto.getName() != null && entrantDto.getNumber() != null)
                        && entrantDtos.size() == 10)
                .verifyComplete();

        verify(crawUtils, never()).saveFailedCrawlRace(serviceToTest.getClass().getName(), raceDto, LocalDate.now());
    }

}