package com.tvf.clb.socket.socket;

import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.ConnectListener;
import com.corundumstudio.socketio.listener.DataListener;
import com.corundumstudio.socketio.listener.DisconnectListener;
import com.tvf.clb.base.dto.RaceResponseDto;
import com.tvf.clb.base.utils.CommonUtils;
import com.tvf.clb.service.service.RaceService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import javax.annotation.PostConstruct;
import java.time.Duration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
@Getter
public class SocketModule {

    private final SocketIOServer server;

    @Autowired
    private RaceService raceService;

    private final Map<Long, Set<SocketIOClient>> raceSubscribers = new ConcurrentHashMap<>();

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
                Set<SocketIOClient> listClient = new HashSet<>();
                listClient.add(senderClient);
                raceSubscribers.put(raceId, listClient);
            }

            if (subscribedRaces.containsKey(raceId)) {
                RaceResponseDto subscribedRace = subscribedRaces.get(raceId);

                senderClient.sendEvent("new_prices", subscribedRace.getEntrants());
                senderClient.sendEvent("new_status", subscribedRace.getStatus());

                if (subscribedRace.getActualStart() != null) {
                    senderClient.sendEvent("new_actual_start", subscribedRace.getActualStart());
                }

                if (CollectionUtils.isEmpty(subscribedRace.getFinalResult())) {
                    senderClient.sendEvent("new_final_result", subscribedRace.getFinalResult());
                }
            }

        };
    }

    private DataListener<Long> unsubscribe() {
        return (senderClient, raceId, ackSender) -> {
            String sessionId = senderClient.getSessionId().toString();

            Set<SocketIOClient> clients = raceSubscribers.get(raceId);
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
    public void crawlSubscribedRaces() {
        Flux.interval(Duration.ofSeconds(4L))
                .flatMap(tick -> Flux.fromIterable(raceSubscribers.keySet()))
                .parallel()
                .runOn(Schedulers.parallel())
                .subscribe(this::getRaceInfoAndSendToClient);
    }

    private void getRaceInfoAndSendToClient(Long raceId) {
        raceService.getRaceNewDataById(raceId)
                .subscribe(newRaceInfo -> {
                    Set<SocketIOClient> clients = raceSubscribers.get(raceId);

                    if (clients != null) {
                        clients.forEach(client -> {
                            client.sendEvent("new_prices", newRaceInfo.getEntrants());
                            log.info("Send race[id={}] new price to client ID[{}]", raceId, client.getSessionId().toString());
                        });

                        RaceResponseDto subscribedRace = subscribedRaces.get(raceId);

                        String newStatus = newRaceInfo.getStatus();
                        if (newStatus != null && (subscribedRace == null || ! newStatus.equals(subscribedRace.getStatus()))) {
                            clients.forEach(client -> {
                                client.sendEvent("new_status", newStatus);
                                log.info("Send race[id={}] new status to client ID[{}]", raceId, client.getSessionId().toString());
                            });
                        }

                        String newActualStart = newRaceInfo.getActualStart();
                        if (newActualStart != null && (subscribedRace == null || ! newActualStart.equals(subscribedRace.getActualStart()))) {
                            clients.forEach(client -> {
                                client.sendEvent("new_actual_start", newActualStart);
                                log.info("Send race[id={}] new actual start to client ID[{}]", raceId, client.getSessionId().toString());
                            });
                        }

                        Map<Integer, String> newFinalResult = newRaceInfo.getFinalResult();
                        if (newFinalResult != null &&
                                (subscribedRace == null || subscribedRace.getFinalResult() == null
                                    || subscribedRace.getFinalResult().size() != newFinalResult.size()))
                        {
                            clients.forEach(client -> {
                                client.sendEvent("new_final_result", newFinalResult);
                                log.info("Send race[id={}] new final result to client ID[{}]", raceId, client.getSessionId().toString());
                            });
                        }

                        subscribedRaces.put(raceId, newRaceInfo);

                        if (CommonUtils.isRaceFinalOrAbandonedInAllSite(newRaceInfo)) {
                            clients.forEach(client -> client.sendEvent("subscription", "Race completed or abandoned"));
                            raceSubscribers.remove(raceId);
                            subscribedRaces.remove(raceId);
                        }
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

            Iterator<Map.Entry<Long, Set<SocketIOClient>>> iterator = raceSubscribers.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<Long, Set<SocketIOClient>> entry = iterator.next();
                entry.getValue().remove(client);
                if (entry.getValue().isEmpty()) {
                    subscribedRaces.remove(entry.getKey());
                    iterator.remove();
                }
            }
        };
    }

}
