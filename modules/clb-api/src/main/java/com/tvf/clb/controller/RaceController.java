package com.tvf.clb.controller;

import com.tvf.clb.base.dto.RaceBaseResponseDTO;
import com.tvf.clb.base.dto.RaceResponseDto;
import com.tvf.clb.base.dto.RaceEntrantDto;
import com.tvf.clb.base.entity.Race;
import com.tvf.clb.base.utils.CommonUtils;
import com.tvf.clb.service.service.RaceService;
import com.tvf.clb.service.service.RaceType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/race")
public class RaceController {

    @Autowired
    private RaceService raceService;


    @GetMapping("")
    public Mono<Race> getRaceById(@RequestParam(value = "id") Long id) {
        return raceService.getRaceById(id);
    }

    @GetMapping("/new-data")
    public Mono<RaceResponseDto> getRaceNewDataById(@RequestParam(value = "id") Long id) {
        return raceService.getRaceNewDataById(id);
    }

    @GetMapping("/side-bar-races")
    public Flux<RaceBaseResponseDTO> searchRaces(@RequestParam(value = "date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                                 @RequestParam(value = "meetingIds", required = false) List<Long> meetingIds,
                                                 @RequestParam(value = "raceTypes", required = false, defaultValue = "HORSE,GREYHOUND,HARNESS") List<RaceType> raceTypes) {
        return raceService.searchRaces(date, meetingIds, raceTypes);
    }

    @GetMapping("/get-meeting-race-number")
    public Flux<Race> getMeetingRaceNumberByRaceId(@RequestParam(value = "id") Long id) {
        return raceService.findAllRacesInSameMeetingByRaceId(id);
    }

    @GetMapping("/entrant")
    public Mono<ResponseEntity<RaceEntrantDto>> getRaceEntrantByRaceId(@RequestParam("id") Long id) {
        return raceService.getRaceEntrantByRaceId(id).map(raceIdAndStatus -> {
            if (raceIdAndStatus == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok().body(raceIdAndStatus);
        });
    }
    @GetMapping("/side-bar-races-default")
    public Flux<RaceBaseResponseDTO> searchRacesByDate(@RequestParam(value = "date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date){
        return raceService.getListRaceDefault(date);
    }

    @GetMapping("/status")
    public Mono<ResponseEntity<Map<Long, String>>> getAllStatusRaceIds(@RequestParam String ids) {
        return raceService.mapRaceIdAndStatusFromDbOrRedis(CommonUtils.convertStringToListLong(ids))
                .map(raceIdAndStatus -> {
                    if (CollectionUtils.isEmpty(raceIdAndStatus)) {
                        return ResponseEntity.notFound().build();
                    }
                    return ResponseEntity.ok().body(raceIdAndStatus);
                });
    }

}
