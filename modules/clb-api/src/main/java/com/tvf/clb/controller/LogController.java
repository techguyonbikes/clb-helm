package com.tvf.clb.controller;

import com.tvf.clb.base.dto.LogLevel;
import com.tvf.clb.service.service.LogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.time.LocalDate;

@RestController
@RequestMapping(value = "/logs")
public class LogController {

    @Autowired
    private LogService logService;

    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamLogs() {
        return logService.streamLog();
    }

    @GetMapping(value = "/{date}")
    public String getLogByDate(@PathVariable("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                               @RequestParam(value = "level", required = false) LogLevel logLevel) {
        return logService.getLogByDate(date, logLevel);
    }

    @GetMapping(value = "/{date}/{hour}")
    public String getLogByDateAndHour(@PathVariable("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date, @PathVariable("hour") String hour,
                                      @RequestParam(value = "level", required = false) LogLevel logLevel) {
        return logService.getLogByDateAndHour(date, hour, logLevel);
    }

}