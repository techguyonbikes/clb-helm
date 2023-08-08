package com.tvf.clb.service;

import com.tvf.clb.base.dto.SiteEnum;
import com.tvf.clb.base.exception.ApiRequestFailedException;
import com.tvf.clb.base.utils.AppConstant;
import com.tvf.clb.base.utils.ConvertBase;
import com.tvf.clb.service.service.CrawUtils;
import com.tvf.clb.service.service.TopSportCrawlService;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.*;

class TopSportCrawlServiceTest {


    @Mock
    private CrawUtils crawUtils;

    @Mock
    private WebClient topSportWebClient;

    @InjectMocks
    private TopSportCrawlService serviceToTest;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void tearDown() {
    }

    Document getDataCrawl(String fileURL) throws IOException {
        return Jsoup.parse(new File(fileURL));
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
        String meetingQueryURI = AppConstant.TOPSPORT_MEETING_QUERY.replace(AppConstant.DATE_PARAM, ConvertBase.getDateOfWeek(date));
        String className = serviceToTest.getClass().getName();

        Mono<Document> resultMono = Mono.error(new ApiRequestFailedException());

        when(crawUtils.crawlData(topSportWebClient, meetingQueryURI, Document.class, className, 5L)).thenReturn(resultMono);

        StepVerifier.create(serviceToTest.getTodayMeetings(date))
                .expectErrorMatches(throwable -> throwable instanceof ApiRequestFailedException)
                .verify();

        verify(crawUtils, times(1)).saveFailedCrawlMeeting(className, date);
    }

    @Test
    void giveNotNullLocalDate_whenGetTodayMeetings_thenReturnFluxMeetingDto() throws IOException {

        String className = serviceToTest.getClass().getName();
        //crawl races
        when(crawUtils.crawlData(eq(topSportWebClient), any(), eq(Document.class), eq(className), any())).thenReturn(Mono.empty());

        String raceQueryURI = "/Racing/Thoroughbreds/Darwin/R4/36977512";
        Mono<Document> raceDto = Mono.just(getDataCrawl("src/test/resources/topsport/TopSportTestRaceInMeetingData.text"));
        when(crawUtils.crawlData(eq(topSportWebClient), eq(raceQueryURI), eq(Document.class), eq(className), any())).thenReturn(raceDto);

        when(crawUtils.saveMeetingSiteAndRaceSite(anyMap(), any())).thenReturn(mock(Mono.class));

        when(crawUtils.getIdForNewRace(any(), any())).thenReturn(Mono.just(1L));

        //crawl meeting
        LocalDate date = LocalDate.now();
        String meetingQueryURI = AppConstant.TOPSPORT_MEETING_QUERY.replace(AppConstant.DATE_PARAM, ConvertBase.getDateOfWeek(date));

        when(crawUtils.crawlData(topSportWebClient, meetingQueryURI, Document.class, serviceToTest.getClass().getName(), 5L))
                .thenReturn(Mono.just(getDataCrawl("src/test/resources/topsport/TopSportMeetingData.text")));


        StepVerifier.create(serviceToTest.getTodayMeetings(date).collectList())
                .expectNextMatches(meetingDtos ->
                        meetingDtos.stream().allMatch(meetingDto -> meetingDto.getId() != null && meetingDto.getName() != null
                        && meetingDto.getAdvertisedDate() != null))
                .verifyComplete();

        verify(crawUtils, never()).saveFailedCrawlMeeting(serviceToTest.getClass().getName(), date);
        verify(crawUtils, times(1)).saveEntrantCrawlDataToRedis(any(), any(), any());
        verify(crawUtils, times(1)).saveEntrantsPriceIntoDB(any(), any(), any());
        verify(crawUtils, times(1)).updateRaceFinalResultIntoDB(any(), any(), any());
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
        Mono<Document> raceDto = Mono.error(new ApiRequestFailedException());

        String raceQueryURI = AppConstant.TOPSPORT_RACE_QUERY.replace(AppConstant.ID_PARAM, raceId);
        when(crawUtils.crawlData(topSportWebClient, raceQueryURI, Document.class, serviceToTest.getClass().getName(), 5L))
                .thenReturn(raceDto);

        StepVerifier.create(serviceToTest.getEntrantByRaceUUID(raceId))
                .expectNextCount(0)
                .verifyComplete();
    }

    @Test
    void giveStatusFinal_whenGetEntrantByRaceUUID_thenReturnMonoCrawlRaceData() throws IOException {
        String raceId = "/Racing/Thoroughbreds/Clairefontaine_-_Fra/R4/36991381";
        Mono<Document> raceDto = Mono.just(getDataCrawl("src/test/resources/topsport/TopSportRaceData.text"));

        String raceQueryURI = AppConstant.TOPSPORT_RACE_QUERY.replace(AppConstant.ID_PARAM, raceId);
        when(crawUtils.crawlData(topSportWebClient, raceQueryURI, Document.class, serviceToTest.getClass().getName(), 5L))
                .thenReturn(raceDto);

        StepVerifier.create(serviceToTest.getEntrantByRaceUUID(raceId))
                .expectNextMatches(crawlRaceData -> SiteEnum.TOP_SPORT.getId() == crawlRaceData.getSiteEnum().getId() &&
                        crawlRaceData.getActualStart() == null &&
                        crawlRaceData.getAdvertisedStart() == null &&
                        crawlRaceData.getMapEntrants().size() == 10
                )
                .verifyComplete();
    }
}
