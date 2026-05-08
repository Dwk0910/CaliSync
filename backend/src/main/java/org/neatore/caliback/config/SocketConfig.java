package org.neatore.caliback.config;

import lombok.RequiredArgsConstructor;

import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

import org.neatore.caliback.handler.ClientHandler;
import org.neatore.caliback.handler.WebServiceHandler;
import org.neatore.caliback.interceptor.WebSocketHandShakeInterceptor;
import org.neatore.caliback.services.AutoUpdateService;
import org.neatore.caliback.services.DBCService;
import org.neatore.caliback.services.LogSenderService;

import org.neatore.caliback.services.UserVerifyService;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class SocketConfig implements WebSocketConfigurer {
    private final UserVerifyService uvs;
    private final LogSenderService logSenderService;
    private final AutoUpdateService autoUpdateService;
    private final DBCService dbcService;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
//        registry.addHandler(new LogViewerHandler(logSenderService), "/caliweb/logviewer").setAllowedOriginPatterns("*")
//                .addInterceptors(new HandshakeInterceptor() {
//                    // Implementation of websocket handshake interceptor: Checks that the requested client is verified, if not, reject the handshake
//                    @Override
//                    public boolean beforeHandshake(@NotNull ServerHttpRequest request, @NotNull ServerHttpResponse response, @NotNull WebSocketHandler wsHandler, @NotNull Map<String, Object> attributes) {
//                        if (request instanceof ServletServerHttpRequest sshr) {
//                            String authorization = sshr.getHeaders().getFirst("Authorization");
//                            return (authorization != null) && uvs.verify(authorization);
//                        }
//                        return false;
//                    }
//
//                    @Override
//                    public void afterHandshake(@NotNull ServerHttpRequest request, @NotNull ServerHttpResponse response, @NotNull WebSocketHandler wsHandler, @Nullable Exception exception) {
//                        // auto-generated
//                    }
//                });
        registry.addHandler(new WebServiceHandler(autoUpdateService), "/caliweb").setAllowedOriginPatterns("*");
        registry.addHandler(new ClientHandler(autoUpdateService, dbcService), "/caliclient").setAllowedOriginPatterns("*")
                .addInterceptors(new WebSocketHandShakeInterceptor());
    }
}
