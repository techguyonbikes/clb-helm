package com.tvf.clb.api.config;

import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;

@Component
public class RejectedTaskHandler implements RejectedExecutionHandler {
    private final org.slf4j.Logger log = LoggerFactory.getLogger(this.getClass());

    @Override
    public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
        log.warn("RejectedTaskHandler: The task " + r + " has been rejected");
        log.info("Retry task " + r);
        if (!executor.isShutdown()) {
            try {
                executor.getQueue().put(r);
            } catch (InterruptedException e) {
                log.error(e.toString());
            }
        }
    }
}
