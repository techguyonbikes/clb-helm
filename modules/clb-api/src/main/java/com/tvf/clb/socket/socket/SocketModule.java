package com.tvf.clb.socket.socket;

import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.ConnectListener;
import com.corundumstudio.socketio.listener.DataListener;
import com.corundumstudio.socketio.listener.DisconnectListener;
import com.tvf.clb.base.dto.RaceResponseDto;
import com.tvf.clb.base.utils.AppConstant;
import com.tvf.clb.service.service.RaceRedisService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
@Slf4j
public class SocketModule {

    private final SocketIOServer server;

    @Autowired
    private RaceRedisService raceRedisService;

    private final Map<Long, List<SocketIOClient>> raceSubscribers = new ConcurrentHashMap<>();

    private final Map<Long, RaceResponseDto> subscribedRaces = new ConcurrentHashMap<>();


    public SocketModule(SocketIOServer server) {
        this.server = server;
        server.addConnectListener(onConnected());
        server.addDisconnectListener(onDisconnected());
        server.addEventListener("call_phat_xem_nao", Long.class, subscribe());
        server.addEventListener("unsubscribe", Long.class, unsubscribe());
    }

    private DataListener<Long> subscribe() {
        return (senderClient, raceId, ackSender) -> {
            log.info("Socket ID[{}] - Subscribed to raceId: {}", senderClient.getSessionId().toString(), raceId);
            if (raceSubscribers.containsKey(raceId)) {
                raceSubscribers.get(raceId).add(senderClient);
            } else {
                List<SocketIOClient> listClient = new CopyOnWriteArrayList<>();
                listClient.add(senderClient);
                raceSubscribers.put(raceId, listClient);
            }
        };
    }

    private DataListener<Long> unsubscribe() {
        return (senderClient, raceId, ackSender) -> {
            String sessionId = senderClient.getSessionId().toString();

            List<SocketIOClient> clients = raceSubscribers.get(raceId);
            if (clients != null) {
                clients.remove(senderClient);

                if (clients.isEmpty()) { // remove race from map if no client subscribes it
                    raceSubscribers.remove(raceId);
                    subscribedRaces.remove(raceId);
                }
            }

            log.info("Socket ID[{}] - Unsubscribed from raceId: {}", sessionId, raceId);
        };
    }

    /**
     * This function get subscribed races from redis and send to clients each 5 seconds
     */
    @PostConstruct
    public void getSubscribedRaces() {
        Flux.interval(Duration.ofSeconds(5L))
                .flatMap(tick -> Flux.fromIterable(raceSubscribers.keySet()))
                .parallel()
                .runOn(Schedulers.parallel())
                .subscribe(this::getRaceInfoAndSendToClient);
    }

    private void getRaceInfoAndSendToClient(Long raceId) {
        raceRedisService.findByRaceId(raceId)
                .subscribe(newRaceInfo -> {
                    List<SocketIOClient> clients = raceSubscribers.get(raceId);

                    if (subscribedRaces.get(raceId) == null) {
                        clients.forEach(client -> {
                            client.sendEvent("new_prices", newRaceInfo.getEntrants());
                            client.sendEvent("new_status", newRaceInfo.getStatus());
                        });
                    } else {
                        if (subscribedRaces.get(raceId).getStatus().equals(newRaceInfo.getStatus())) {
                            clients.forEach(client -> client.sendEvent("new_status", newRaceInfo.getStatus()));
                        }
                        clients.forEach(client -> client.sendEvent("new_prices", newRaceInfo.getEntrants()));
                    }

                    subscribedRaces.put(raceId, newRaceInfo);

                    if (newRaceInfo.getStatus().equals(AppConstant.STATUS_FINAL) || newRaceInfo.getStatus().equals(AppConstant.STATUS_ABANDONED)) {
                        clients.forEach(client -> client.sendEvent("subscription", "race completed or abandoned"));
                        raceSubscribers.remove(raceId);
                        subscribedRaces.remove(raceId);
                    }
                });

    }

    private ConnectListener onConnected() {
        return (client) -> {
            client.sendEvent("subscription", "connected");
            log.info("Socket ID[{}] -  Connected ", client.getSessionId().toString());
        };

    }

    private DisconnectListener onDisconnected() {
        return client -> {
            log.info("Socket ID[{}] -  disconnected", client.getSessionId().toString());
            log.info("Number of clients left: {}" , server.getAllClients().size());
        };
    }

}
