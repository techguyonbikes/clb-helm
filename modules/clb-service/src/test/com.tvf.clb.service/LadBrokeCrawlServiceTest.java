package com.tvf.clb.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tvf.clb.base.dto.MeetingAndSiteUUID;
import com.tvf.clb.base.entity.Entrant;
import com.tvf.clb.base.entity.Meeting;
import com.tvf.clb.base.entity.Race;
import com.tvf.clb.service.service.TodayData;
import com.tvf.clb.base.exception.ApiRequestFailedException;
import com.tvf.clb.base.kafka.service.CloudbetKafkaService;
import com.tvf.clb.base.model.ladbrokes.LadBrokedItMeetingDto;
import com.tvf.clb.base.model.ladbrokes.LadBrokedItRaceDto;
import com.tvf.clb.base.model.ladbrokes.LadbrokesRaceApiResponse;
import com.tvf.clb.base.utils.AppConstant;
import com.tvf.clb.base.utils.InstantDeserializer;
import com.tvf.clb.service.repository.EntrantRepository;
import com.tvf.clb.service.repository.MeetingRepository;
import com.tvf.clb.service.repository.RaceRepository;
import com.tvf.clb.service.service.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class LadBrokeCrawlServiceTest {
    @Mock
    private MeetingRepository meetingRepository;

    @Mock
    private RaceRepository raceRepository;

    @Mock
    private EntrantRepository entrantRepository;

    @Mock
    private CrawUtils crawUtils;

    @Mock
    private ServiceLookup serviceLookup;

    @Mock
    private RaceRedisService raceRedisService;

    @Mock
    private TodayData todayData;

    @Mock
    private CloudbetKafkaService kafkaService;

    @Mock
    private WebClient ladbrokesWebClient;

    @InjectMocks
    private LadBrokeCrawlService serviceToTest;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    void giveNullDate_whenGetTodayMeetings_thenThrowsNullPointerException() {
        LocalDate date = null;
        Assertions.assertThrows(NullPointerException.class, () -> serviceToTest.getTodayMeetings(date).subscribe());
    }

    @Test
    void giveNotNullDate_andCrawlMeetingDataSuccess_andCrawlRaceBothFailAndSuccess_whenGetTodayMeetings_thenReturnMeetingDtoFlux() throws IOException {
        LocalDate date = LocalDate.now();
        String meetingQueryURI = AppConstant.LAD_BROKES_IT_MEETING_QUERY.replace(AppConstant.DATE_PARAM, date.toString());
        String className = serviceToTest.getClass().getName();

        ObjectMapper objectMapper = new ObjectMapper();
        LadBrokedItMeetingDto ladBrokedItMeetingDto = objectMapper.readValue(new File("src/test/resources/ladbrokes/ladbroke-meeting-api-response.json"), LadBrokedItMeetingDto.class);

        when(crawUtils.crawlData(ladbrokesWebClient, meetingQueryURI, LadBrokedItMeetingDto.class, className, 5L)).thenReturn(Mono.just(ladBrokedItMeetingDto));

        // prepare data for function 'saveMeetingAndRace'
        MeetingAndSiteUUID meetingAndSiteUUID = new MeetingAndSiteUUID();
        meetingAndSiteUUID.setSiteUUID("00fd42ea-21c1-4d69-bcd9-5e36aee80273");
        List<MeetingAndSiteUUID> listExistingMeeting = Collections.singletonList(meetingAndSiteUUID);

        when(meetingRepository.findAllByMeetingUUIDInAndSiteId(any(), any())).thenReturn(Flux.fromIterable(listExistingMeeting));

        when(meetingRepository.save(any(Meeting.class))).thenAnswer(invocationOnMock -> {
            Meeting result = (Meeting) invocationOnMock.getArguments()[0];
            result.setId(2 + (long) (Math.random() * 1000));
            return Mono.just(result);
        });
        when(meetingRepository.save(Mockito.argThat(meeting -> meeting.getMeetingId().equals("09cbdd54-5d5a-43fc-ba54-1e036715ce6f")))).thenAnswer(invocationOnMock -> {
            Meeting result = (Meeting) invocationOnMock.getArguments()[0];
            result.setId(1L);
            return Mono.just(result);
        });

        when(crawUtils.saveMeetingSite(any(), any(), any())).thenReturn(Flux.empty());

        // prepare data for function 'saveRace'
        List<Race> listExistingRaces = Arrays.asList(
            Race.builder().meetingId(1L).number(1).build(),
            Race.builder().meetingId(1L).number(2).build(),
            Race.builder().meetingId(1L).number(3).build()
        );

        when(crawUtils.saveRaceSite(any(), any())).thenReturn(Flux.empty());

        when(raceRepository.findAllByNumberInAndMeetingIdIn(any(), any())).thenReturn(Flux.fromIterable(listExistingRaces));
        when(raceRepository.save(any(Race.class))).thenAnswer(invocationOnMock -> {
            Race race = (Race) invocationOnMock.getArguments()[0];
            race.setId(1 + (long) (Math.random() * 10000));
            return Mono.just(race);
        });
        when(raceRepository.updateDistanceAndResultsDisplayAndSilkUrlAndFullFormUrlById(any(), any(), any(), any(), any())).thenReturn(Mono.empty());

        TreeMap<Long, List<Long>> todayRaces = new TreeMap<>();
        todayRaces.put(Instant.now().toEpochMilli(), Arrays.asList(1L, 2L));
        when(todayData.getRaces()).thenReturn(todayRaces);

        // prepare data for function 'crawlAndSaveEntrants'
        List<Entrant> listExistingEntrants = IntStream.range(1, 5)
                                                      .mapToObj(i -> Entrant.builder().name("testEntrant-" + i).number(i).build())
                                                      .collect(Collectors.toList());
        when(entrantRepository.findByRaceId(any())).thenReturn(Flux.fromIterable(listExistingEntrants));
        when(entrantRepository.saveAll(anyCollection())).thenAnswer(invocationOnMock -> {
            Collection<Entrant> collection = (Collection<Entrant>) invocationOnMock.getArguments()[0];
            collection.forEach(entrant -> entrant.setId(1 + (long) (Math.random() * 10000)));
            return Flux.fromIterable(collection);
        });

        LadbrokesRaceApiResponse ladbrokesRaceApiResponse = getObjectMapper().readValue(new File("src/test/resources/ladbrokes/ladbroke-race-api-response.json"), LadbrokesRaceApiResponse.class);

        when(crawUtils.crawlData(eq(ladbrokesWebClient), any(),
                eq(LadbrokesRaceApiResponse.class), eq(className), any())).thenReturn(Mono.empty());

        String raceReturnResult = AppConstant.LAD_BROKES_IT_RACE_QUERY.replace(AppConstant.ID_PARAM, "c8df6b51-6939-40af-a387-97d8fca785be");
        when(crawUtils.crawlData(eq(ladbrokesWebClient), eq(raceReturnResult), eq(LadbrokesRaceApiResponse.class), eq(className), any())).thenReturn(Mono.just(ladbrokesRaceApiResponse));

        LadbrokesRaceApiResponse ladbrokesRaceApiResponseWithoutPosition = getObjectMapper().readValue(new File("src/test/resources/ladbrokes/ladbroke-race-api-response-without-results.json"), LadbrokesRaceApiResponse.class);
        String raceReturnResultWithoutPosition = AppConstant.LAD_BROKES_IT_RACE_QUERY.replace(AppConstant.ID_PARAM, "70b08d94-fa62-4bd7-ba58-46e0e6b0dbf3");
        when(crawUtils.crawlData(eq(ladbrokesWebClient), eq(raceReturnResultWithoutPosition), eq(LadbrokesRaceApiResponse.class), eq(className), any())).thenReturn(Mono.just(ladbrokesRaceApiResponseWithoutPosition));

        String raceThrowException = AppConstant.LAD_BROKES_IT_RACE_QUERY.replace(AppConstant.ID_PARAM, "11758832-c752-496b-a0b3-867349045690");
        when(crawUtils.crawlData(eq(ladbrokesWebClient), eq(raceThrowException), eq(LadbrokesRaceApiResponse.class), eq(className), any())).thenReturn(Mono.error(new NullPointerException("null point exception")));

        String raceCanNotCrawl = AppConstant.LAD_BROKES_IT_RACE_QUERY.replace(AppConstant.ID_PARAM, "435881af-abf2-46c2-9519-ddcb10346fe1");
        when(crawUtils.crawlData(eq(ladbrokesWebClient), eq(raceCanNotCrawl), eq(LadbrokesRaceApiResponse.class), eq(className), any())).thenReturn(Mono.error(new ApiRequestFailedException()));

        String raceEmptyData = AppConstant.LAD_BROKES_IT_RACE_QUERY.replace(AppConstant.ID_PARAM, "855bf98f-dc2a-42a6-b884-9c414b4bec16");

        LadbrokesRaceApiResponse ladbrokesRaceApiResponseEmptyData = new LadbrokesRaceApiResponse();
        ladbrokesRaceApiResponseEmptyData.setData(new LadBrokedItRaceDto());
        when(crawUtils.crawlData(eq(ladbrokesWebClient), eq(raceEmptyData), eq(LadbrokesRaceApiResponse.class), eq(className), any())).thenReturn(Mono.just(ladbrokesRaceApiResponseEmptyData));

        when(raceRedisService.saveRace(any(), any())).thenReturn(Mono.just(Boolean.TRUE));
        when(raceRedisService.updateRace(any(), any())).thenReturn(Mono.just(Boolean.TRUE));
        when(raceRedisService.hasKey(any())).thenReturn(Mono.just(Boolean.TRUE));

        ICrawlService iCrawlService = mock(ICrawlService.class);
        when(serviceLookup.forBean(any(), any())).thenReturn(iCrawlService);
        when(iCrawlService.getTodayMeetings(any())).thenReturn(Flux.empty());

        StepVerifier.create(serviceToTest.getTodayMeetings(date).collectList())
                .expectNextMatches(meetingDtos -> meetingDtos.stream().allMatch(meetingDto -> meetingDto.getRaces().size() > 0 && meetingDto.getId() != null))
                .verifyComplete();

        verify(todayData, times(1)).setLastTimeCrawl(any());
        verify(crawUtils, times(1)).saveFailedCrawlMeeting(className, date);
        verify(meetingRepository, times(139)).save(any(Meeting.class));
        verify(raceRepository, times(1371)).save(any(Race.class));
        verify(kafkaService, times(1526)).publishKafka(any(), any(), isNull()); // equals number of (meetings + races + entrants need to update)
        verify(raceRedisService, times(2)).updateRace(any(), any());
        verify(entrantRepository, times(2)).saveAll(anyCollection());
        verify(crawUtils, times(1)).saveMeetingSite(any(), any(), any());
        verify(crawUtils, times(1)).saveRaceSite(any(), anyInt());
    }

    @Test
    void giveNotNullDate_andCrawlMeetingDataFail_whenGetTodayMeetings_thenApiRequestFailedError() {
        LocalDate date = LocalDate.now();
        String meetingQueryURI = AppConstant.LAD_BROKES_IT_MEETING_QUERY.replace(AppConstant.DATE_PARAM, date.toString());
        String className = serviceToTest.getClass().getName();

        when(crawUtils.crawlData(ladbrokesWebClient, meetingQueryURI, LadBrokedItMeetingDto.class, className, 5L)).thenReturn(Mono.error(new ApiRequestFailedException()));

        StepVerifier.create(serviceToTest.getTodayMeetings(date))
                .expectErrorMatches(throwable -> throwable instanceof ApiRequestFailedException)
                .verify();

        verify(crawUtils).saveFailedCrawlMeeting(className, date);
        verify(todayData, never()).setLastTimeCrawl(any());
    }

    @Test
    void crawlRaceDataFail_whenGetEntrantByRaceUUID_thenReturnEmpty() {
        String raceId = "c8df6b51-6939-40af-a387-97d8fca785be";
        String className = serviceToTest.getClass().getName();
        when(crawUtils.crawlData(eq(ladbrokesWebClient), any(), eq(LadbrokesRaceApiResponse.class), eq(className), any())).thenReturn(Mono.error(new ApiRequestFailedException()));

        StepVerifier.create(serviceToTest.getEntrantByRaceUUID(raceId))
                .expectNextCount(0)
                .verifyComplete();
    }

    @Test
    void crawlRaceDataSuccess_andResponseNotContainRaceAndEntrantData_whenGetEntrantByRaceUUID_thenReturnEmpty() {
        String raceId = "c8df6b51-6939-40af-a387-97d8fca785be";
        String className = serviceToTest.getClass().getName();
        when(crawUtils.crawlData(eq(ladbrokesWebClient), any(), eq(LadbrokesRaceApiResponse.class), eq(className), any())).thenReturn(Mono.just(new LadbrokesRaceApiResponse()));

        StepVerifier.create(serviceToTest.getEntrantByRaceUUID(raceId))
                .expectNextCount(0)
                .verifyComplete();
    }

    @Test
    void crawlRaceDataSuccess_andResponseContainRaceAndEntrantData_whenGetEntrantByRaceUUID_thenReturnResult() throws IOException {
        String raceId = "c8df6b51-6939-40af-a387-97d8fca785be";
        String className = serviceToTest.getClass().getName();
        String top4Entrants = "5,1,2,7";

        LadbrokesRaceApiResponse ladbrokesRaceApiResponse = getObjectMapper().readValue(new File("src/test/resources/ladbrokes/ladbroke-race-api-response.json"), LadbrokesRaceApiResponse.class);
        when(crawUtils.crawlData(eq(ladbrokesWebClient), any(), eq(LadbrokesRaceApiResponse.class), eq(className), any())).thenReturn(Mono.just(ladbrokesRaceApiResponse));

        StepVerifier.create(serviceToTest.getEntrantByRaceUUID(raceId))
                    .expectNextMatches(crawlRaceData ->
                            crawlRaceData.getStatus().equals(AppConstant.STATUS_FINAL)
                            && crawlRaceData.getMapEntrants().values().stream().allMatch(entrant -> entrant.getPriceMap().size() > 0 && entrant.getPosition() != null)
                            && crawlRaceData.getFinalResult().get(1).equals(top4Entrants))
                    .verifyComplete();
    }

    @Test
    void crawlRaceDataSuccess_andResponseContainRaceAndEntrantData_andRaceNotResulted_whenGetEntrantByRaceUUID_thenReturnResult() throws IOException {
        String raceId = "70b08d94-fa62-4bd7-ba58-46e0e6b0dbf3";
        String className = serviceToTest.getClass().getName();

        LadbrokesRaceApiResponse ladbrokesRaceApiResponse = getObjectMapper().readValue(new File("src/test/resources/ladbrokes/ladbroke-race-api-response-without-results.json"), LadbrokesRaceApiResponse.class);
        when(crawUtils.crawlData(eq(ladbrokesWebClient), any(), eq(LadbrokesRaceApiResponse.class), eq(className), any())).thenReturn(Mono.just(ladbrokesRaceApiResponse));

        StepVerifier.create(serviceToTest.getEntrantByRaceUUID(raceId))
                .expectNextMatches(crawlRaceData ->
                        crawlRaceData.getStatus().equals(AppConstant.STATUS_OPEN)
                                && crawlRaceData.getMapEntrants().values().stream().allMatch(entrant -> entrant.getPriceMap().size() > 0 && entrant.getPosition() == 0)
                                && crawlRaceData.getFinalResult() == null)
                .verifyComplete();
    }

    public ObjectMapper getObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        JavaTimeModule module = new JavaTimeModule();
        module.addDeserializer(Instant.class, new InstantDeserializer());
        mapper.registerModule(module);
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        return mapper;
    }
}