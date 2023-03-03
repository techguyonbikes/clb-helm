package com.tvf.clb.ws.socket;

import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.ConnectListener;
import com.corundumstudio.socketio.listener.DataListener;
import com.corundumstudio.socketio.listener.DisconnectListener;
import com.tvf.clb.base.dto.EntrantMapper;
import com.tvf.clb.base.dto.EntrantResponseDto;
import com.tvf.clb.base.entity.Entrant;
import com.tvf.clb.service.service.EntrantService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class SocketModule {

    private final SocketIOServer server;

    @Autowired
    private EntrantService entrantService;

    public SocketModule(SocketIOServer server) {
        this.server = server;
        server.addConnectListener(onConnected());
        server.addDisconnectListener(onDisconnected());
        server.addEventListener("ping", String.class, healthCheck());
    }

    private DataListener<String> healthCheck() {
        return (senderClient, data, ackSender) -> {
            log.info(data.toString());
            sendPong(senderClient, data);
        };
    }

    private void sendPong(SocketIOClient senderClient, String request) {
        for (SocketIOClient client : senderClient.getNamespace().getAllClients()) {
            client.sendEvent("ping", "pong");
        }
    }

    private ConnectListener onConnected() {
        return (client) -> {
            java.util.Map<String, java.util.List<String>> params = client.getHandshakeData().getUrlParams();
            client.sendEvent("subscription", "new prices");
            log.info("Socket ID[{}] -  Connected ", client.getSessionId().toString());
        };

    }

    private DisconnectListener onDisconnected() {
        return client -> {
            java.util.Map<String, java.util.List<String>> params = client.getHandshakeData().getUrlParams();
            log.info("Socket ID[{}] -  disconnected", client.getSessionId().toString());
        };
    }

    @Scheduled(cron = "0/20 * * * * *")
    public void  sendDate() {
        List<SocketIOClient> clients = new ArrayList<>(server.getAllClients());
        Flux<Entrant> entrants =  entrantService.getEntrantsByRaceId("1c255ce2-3bf6-4322-85ad-f2c9395aebde");
        List<EntrantResponseDto> entrantList= entrants.map(EntrantMapper::toEntrantResponseDto).collectList().block();
//                    entrantList.subscribe();
        clients.forEach(x -> {
            x.sendEvent("new_prices", entrantList);
        });

    }

}
