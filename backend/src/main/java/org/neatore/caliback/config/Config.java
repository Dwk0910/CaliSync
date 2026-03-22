package org.neatore.caliback.config;

import lombok.RequiredArgsConstructor;
import org.neatore.caliback.handler.ClientHandler;
import org.neatore.caliback.handler.WebServiceHandler;
import org.neatore.caliback.interceptor.WebSocketHandShakeInterceptor;
import org.neatore.caliback.services.AutoUpdateService;
import org.neatore.caliback.services.DBCService;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class Config implements WebSocketConfigurer {
    private final AutoUpdateService autoUpdateService;
    private final DBCService dbcService;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(new WebServiceHandler(autoUpdateService), "/caliweb").setAllowedOriginPatterns("*");
        registry.addHandler(new ClientHandler(autoUpdateService, dbcService), "/caliclient").setAllowedOriginPatterns("*")
                .addInterceptors(new WebSocketHandShakeInterceptor());
    }
}
