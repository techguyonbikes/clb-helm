package com.tvf.clb.service.service;

import com.google.gson.Gson;
import com.tvf.clb.base.entity.FailedApiCall;
import com.tvf.clb.service.repository.FailedApiCallRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.Map;

@Service
@Slf4j
public class FailedApiCallService {

    @Autowired
    private FailedApiCallRepository failedApiCallRepository;

    public void saveFailedApiCallInfoToDB(String className, String methodName, Map<String, String> params) {

        String paramJson = new Gson().toJson(params);

        FailedApiCall failedApiCall = new FailedApiCall();
        failedApiCall.setClassName(className);
        failedApiCall.setMethodName(methodName);
        failedApiCall.setParams(paramJson);
        failedApiCall.setFailedTime(Instant.now());

        failedApiCallRepository.findByClassNameAndMethodNameAndParams(className, methodName, paramJson)
                .switchIfEmpty(failedApiCallRepository.save(failedApiCall))
                .subscribe();
    }

    public Flux<FailedApiCall> getAllTodayFailedApiCall() {
        Instant startOfToday = Instant.now().atZone(ZoneOffset.UTC).with(LocalTime.MIN).toInstant();
        Instant endOfToday = Instant.now().atZone(ZoneOffset.UTC).with(LocalTime.MAX).toInstant();

        return failedApiCallRepository.findByFailedTimeBetween(startOfToday, endOfToday);
    }

    public void removeById(Long id) {
        failedApiCallRepository.deleteById(id).subscribe();
    }
}
