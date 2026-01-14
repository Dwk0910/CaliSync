package org.neatore.caliback;

import org.neatore.caliback.handler.Handler;
import org.neatore.caliback.services.DBCService;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class Config implements WebSocketConfigurer {
    private final DBCService dbcService;

    public Config(DBCService dbcService) {
        this.dbcService = dbcService;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(new Handler(dbcService), "/calisync").setAllowedOrigins("*");
    }
}
