package org.neatore.caliback;

import org.neatore.caliback.handler.ClientHandler;
import org.neatore.caliback.handler.WebServiceHandler;
import org.neatore.caliback.services.AutoUpdateService;
import org.neatore.caliback.services.DBCService;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class Config implements WebSocketConfigurer {
    private final AutoUpdateService autoUpdateService;
    private final DBCService dbcService;

    public Config(AutoUpdateService autoUpdateService, DBCService dbcService) {
        this.dbcService = dbcService;
        this.autoUpdateService = autoUpdateService;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(new ClientHandler(autoUpdateService, dbcService), "/caliclient").setAllowedOrigins("*");
        registry.addHandler(new WebServiceHandler(autoUpdateService), "/caliweb").setAllowedOrigins("*");
    }
}
