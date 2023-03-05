package com.tvf.clb.socket.socket;

import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.ConnectListener;
import com.corundumstudio.socketio.listener.DataListener;
import com.corundumstudio.socketio.listener.DisconnectListener;
import com.tvf.clb.base.dto.EntrantMapper;
import com.tvf.clb.service.service.EntrantService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class SocketModule {

    private final SocketIOServer server;

    private final Map<String, Disposable> subscriptions = new ConcurrentHashMap<>();

    @Autowired
    private EntrantService entrantService;

    public SocketModule(SocketIOServer server) {
        this.server = server;
        server.addConnectListener(onConnected());
        server.addDisconnectListener(onDisconnected());
        server.addEventListener("call_phat_xem_nao", String.class, subscribe());
        server.addEventListener("unsubscribe", String.class, unsubscribe());
    }

    private DataListener<String> getNewPrices() {
        return (senderClient, data, ackSender) -> {
            log.info(data);
            sendNewPrices(senderClient, data);
        };
    }


    private DataListener<String> subscribe() {
        return (senderClient, raceId, ackSender) -> {
            log.info("Socket ID[{}] - Subscribed to raceId: {}", senderClient.getSessionId().toString(), raceId);
            Disposable subscription = sendNewPrices(senderClient, raceId);
            subscriptions.put(senderClient.getSessionId().toString(), subscription);
            senderClient.sendEvent("subscription", "new prices");
        };
    }

    private DataListener<String> unsubscribe() {
        return (senderClient, raceId, ackSender) -> {
            String sessionId = senderClient.getSessionId().toString();
            Disposable subscription = subscriptions.remove(sessionId);
            if (subscription != null) {
                subscription.dispose();
                log.info("Socket ID[{}] - Unsubscribed from raceId: {}", sessionId, raceId);
            }
        };
    }

    private Disposable sendNewPrices(SocketIOClient senderClient, String request) {
            return Mono.delay(Duration.ofSeconds(20L))
                    .flatMap(tick -> entrantService.getEntrantsByRaceId(request)
                            .map(EntrantMapper::toEntrantResponseDto)
                            .collectList()
                    )
                    .subscribe(
                            entrantList -> {
                                senderClient.sendEvent("new_prices", entrantList);
                                sendNewPrices(senderClient, request);
                            },
                            throwable -> {
                                log.error("Socket ID[{}] - Error in subscription: {}", senderClient.getSessionId().toString(), throwable.getMessage());
                            },
                            () -> {
                                log.info("Socket ID[{}] - Subscription complete", senderClient.getSessionId().toString());
                            }
                    );
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

//    @Scheduled(cron = "0/20 * * * * *")
//    public void  sendDate() {
//        List<SocketIOClient> clients = new ArrayList<>(server.getAllClients());
//        Flux<Entrant> entrants =  entrantService.getEntrantsByRaceId("1c255ce2-3bf6-4322-85ad-f2c9395aebde");
//        List<EntrantResponseDto> entrantList= entrants.map(EntrantMapper::toEntrantResponseDto).collectList().block();
////                    entrantList.subscribe();
//        clients.forEach(x -> {
//            x.sendEvent("new_prices", entrantList);
//        });
//
//    }

}
