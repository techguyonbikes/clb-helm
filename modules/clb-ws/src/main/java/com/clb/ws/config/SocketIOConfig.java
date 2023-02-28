package com.clb.ws.config;

import com.corundumstudio.socketio.SocketIOServer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class SocketIOConfig {

    @Value("${socket-server.host:localhost}")
    private String host;

    @Value("${socket-server.port:20002}")
    private Integer port;

    @Bean
    public SocketIOServer socketIOServer() {
        log.info("hello");
        com.corundumstudio.socketio.Configuration config = new com.corundumstudio.socketio.Configuration();
        config.setHostname(host);
        config.setPort(port);
        //  config.setContext("/socket.io");
        return new SocketIOServer(config);
    }

}
