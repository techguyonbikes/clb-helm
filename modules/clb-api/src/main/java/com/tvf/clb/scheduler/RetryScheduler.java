package com.tvf.clb.scheduler;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.tvf.clb.base.entity.FailedApiCall;
import com.tvf.clb.base.entity.TodayData;
import com.tvf.clb.service.service.FailedApiCallService;
import com.tvf.clb.service.service.ServiceLookup;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@Slf4j
public class RetryScheduler {

    @Autowired
    private ServiceLookup serviceLookup;

    @Autowired
    private FailedApiCallService failedApiCallService;

    @Autowired
    private TodayData todayData;

    private boolean isRetrying = false;

    /**
     * Retry all failed jobs every 10 minutes.
     */
    @Scheduled(cron = "0 */10 * ? * *")
    public void retryFailedJob() {

        // Do not retry if another is running
        if (isRetrying || todayData.getLastTimeCrawl().plus(10, ChronoUnit.MINUTES).isAfter(Instant.now())) {
            return;
        }

        log.info("Start retrying failed jobs");
        isRetrying = true;

        Gson gson = new Gson();

        failedApiCallService.getAllTodayFailedApiCall().doOnNext(failedApiCall -> {
            try {
                String targetClassName = failedApiCall.getClassName();
                String targetMethodName = failedApiCall.getMethodName();

                Class<?> targetClass = Class.forName(targetClassName);

                Map<String, String> mapParams = gson.fromJson(failedApiCall.getParams(), new TypeToken<LinkedHashMap<String, String>>(){}.getType());

                int numberOfParams = mapParams.size();

                // Prepare parameter types and values for target method
                Class<?>[] paramsType = new Class<?>[numberOfParams];
                Object[] paramsValue = new Object[numberOfParams];

                int index = 0;
                for (Map.Entry<String, String> entry : mapParams.entrySet()) {
                    String className = entry.getKey();
                    String paramValue = entry.getValue();

                    paramsType[index] = Class.forName(className);
                    paramsValue[index] = new Gson().fromJson(paramValue, paramsType[index]);

                    index++;
                }

                // Execute method
                Method targetMethod = targetClass.getMethod(targetMethodName, paramsType);
                Object returnObject = targetMethod.invoke(serviceLookup.forBean(targetClass), paramsValue);

                checkRetryStatus(returnObject, failedApiCall);

            } catch (ClassNotFoundException e) {
                log.error("Class name not found {}", failedApiCall.getClassName());
            } catch (NoSuchMethodException e) {
                log.error("Method name not found {}", failedApiCall.getClassName());
            } catch (InvocationTargetException e) {
                doOnRetryError(failedApiCall, e);
            } catch (IllegalAccessException e) {
                log.error("Can not access target method {}", failedApiCall.getClassName());
            }
        })
        .doFinally(signalType -> isRetrying = false)
        .subscribe();
    }

    private void checkRetryStatus(Object returnObject, FailedApiCall failedApiCall) {
        // With method return a Publisher (Mono, Flux,...), we need to trigger manually by calling subscribe() method
        if (returnObject instanceof Mono) {
            ((Mono<?>) returnObject)
                    .doOnSuccess(o -> doOnRetrySuccess(failedApiCall))
                    .doOnError(throwable -> doOnRetryError(failedApiCall, throwable))
                    .onErrorComplete()
                    .subscribe();
        } else if (returnObject instanceof Flux) {
            ((Flux<?>) returnObject)
                    .doOnComplete(() -> doOnRetrySuccess(failedApiCall))
                    .doOnError(throwable -> doOnRetryError(failedApiCall, throwable))
                    .onErrorComplete()
                    .subscribe();
        } else { // Execute doOnRetrySuccess if the retry method not return a Publisher and InvocationTargetException is not thrown
            doOnRetrySuccess(failedApiCall);
        }
    }

    private void doOnRetrySuccess(FailedApiCall failedApiCall) {
        log.info("Retry failed job successfully {}", failedApiCall);
        failedApiCallService.removeById(failedApiCall.getId());
    }

    private void doOnRetryError(FailedApiCall failedApiCall, Throwable e) {
        log.error("Retry method {} still got failed (exception dedail: {}), retry after 10 minutes", failedApiCall.getMethodName(), e.getMessage());
    }
}
