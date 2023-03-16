package com.tvf.clb.socket.socket;

import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.ConnectListener;
import com.corundumstudio.socketio.listener.DataListener;
import com.corundumstudio.socketio.listener.DisconnectListener;
import com.tvf.clb.base.dto.EntrantMapper;
import com.tvf.clb.base.dto.EntrantResponseDto;
import com.tvf.clb.base.entity.EntrantRedis;
import com.tvf.clb.base.entity.Race;
import com.tvf.clb.service.service.CrawlPriceService;
import com.tvf.clb.service.service.CrawlService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

@Component
@Slf4j
public class SocketModule {

    private final SocketIOServer server;

    private final Map<String, Disposable> subscriptions = new ConcurrentHashMap<>();

    @Autowired
    private CrawlService crawlService;

    @Autowired
    private CrawlPriceService crawlPriceService;

    public SocketModule(SocketIOServer server) {
        this.server = server;
        server.addConnectListener(onConnected());
        server.addDisconnectListener(onDisconnected());
        server.addEventListener("call_phat_xem_nao", Long.class, subscribe());
        server.addEventListener("unsubscribe", String.class, unsubscribe());
    }

    private DataListener<String> getNewPrices() {
        return (senderClient, data, ackSender) -> {
            log.info(data);
            sendNewPrices(senderClient, data);
        };
    }


    private DataListener<Long> subscribe() {
        return (senderClient, raceId, ackSender) -> {
            log.info("Socket ID[{}] - Subscribed to raceId: {}", senderClient.getSessionId().toString(), raceId);
            Disposable subscription = sendNewPriceFromRedis(senderClient, raceId);
            subscriptions.put(senderClient.getSessionId().toString(), subscription);
            senderClient.sendEvent("subscription", "new prices");
        };
    }

    private DataListener<String> unsubscribe() {
        return (senderClient, raceId, ackSender) -> {
            String sessionId = senderClient.getSessionId().toString();
            Disposable subscription = subscriptions.remove(sessionId);
            senderClient.sendEvent("unsubscribe", raceId);
            if (subscription != null) {
                subscription.dispose();
                log.info("Socket ID[{}] - Unsubscribed from raceId: {}", sessionId, raceId);
            }
        };
    }


    private Disposable sendNewPriceFromRedis(SocketIOClient senderClient, Long request) {

        Predicate<List<EntrantRedis>> stopEmittingCondition = listEntrant -> listEntrant.stream().anyMatch(entrant -> entrant.getStatus().equals(Race.Status.F.toString()));

        return Flux.interval(Duration.ofSeconds(20L))
                .flatMap(tick -> crawlPriceService.crawlPriceByRaceId(request))
                .doOnNext(entrantList -> senderClient.sendEvent("new_prices", entrantList))
                .takeUntil(stopEmittingCondition) // stop emitting when race has finished
                .doOnComplete(() -> {
                    senderClient.sendEvent("subscription", "race has finished");
                    subscriptions.remove(senderClient.getSessionId().toString());
                })
                .subscribe();
    }

    private Disposable sendNewPrices(SocketIOClient senderClient, String request) {

        Predicate<List<EntrantResponseDto>> stopEmittingCondition = entrants -> entrants.stream().anyMatch(entrant -> entrant.getPosition() > 0);

        return Flux.interval(Duration.ofSeconds(20L))
                .flatMap(tick -> crawlService.getEntrantByRaceId(request).map(EntrantMapper::toEntrantResponseDto).collectList())
                .doOnNext(entrantList -> senderClient.sendEvent("new_prices", entrantList))
                .takeWhile(stopEmittingCondition.negate())
                .doOnComplete(() -> {
                    senderClient.sendEvent("subscription", "race has finished");
                    subscriptions.remove(senderClient.getSessionId().toString());
                })
//                    .doOnError(throwable -> log.error("Socket ID[{}] - Error in subscription: {}", senderClient.getSessionId().toString(), throwable.getMessage()))
                .subscribe();
    }

    private ConnectListener onConnected() {
        return (client) -> {
            java.util.Map<String, List<String>> params = client.getHandshakeData().getUrlParams();
            client.sendEvent("subscription", "new prices");
            log.info("Socket ID[{}] -  Connected ", client.getSessionId().toString());
        };

    }

    private DisconnectListener onDisconnected() {
        return client -> {
            java.util.Map<String, List<String>> params = client.getHandshakeData().getUrlParams();
            log.info("Socket ID[{}] -  disconnected", client.getSessionId().toString());
        };
    }

}
