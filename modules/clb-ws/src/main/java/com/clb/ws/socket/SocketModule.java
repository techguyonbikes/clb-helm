package com.clb.ws.socket;

import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.ConnectListener;
import com.corundumstudio.socketio.listener.DataListener;
import com.corundumstudio.socketio.listener.DisconnectListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
@Slf4j
public class SocketModule {

    private final SocketIOServer server;

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
            log.info("Socket ID[{}] -  Connected ", client.getSessionId().toString());
        };

    }

    private DisconnectListener onDisconnected() {
        return client -> {
            java.util.Map<String, java.util.List<String>> params = client.getHandshakeData().getUrlParams();
            log.info("Socket ID[{}] -  disconnected", client.getSessionId().toString());
        };
    }


}
