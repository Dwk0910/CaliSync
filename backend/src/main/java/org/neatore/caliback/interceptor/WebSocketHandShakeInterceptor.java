package org.neatore.caliback.interceptor;

import lombok.RequiredArgsConstructor;

import org.jetbrains.annotations.NotNull;
import org.jspecify.annotations.Nullable;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;

import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@RequiredArgsConstructor
public class WebSocketHandShakeInterceptor implements HandshakeInterceptor {
    @Override
    public boolean beforeHandshake(@NotNull ServerHttpRequest request, @NotNull ServerHttpResponse response, @NotNull WebSocketHandler wsHandler, @NotNull Map<String, Object> attributes) {
        if (request instanceof ServletServerHttpRequest sshr) {
            String secret = sshr.getHeaders().getFirst("X-Client-Secret");
            return secret != null && secret.equals(System.getenv("CALISYNC_CLIENT_SECRET"));
        }

        return false;
    }

    @Override
    public void afterHandshake(@NotNull ServerHttpRequest request, @NotNull ServerHttpResponse response, @NotNull WebSocketHandler wsHandler, @Nullable Exception exception) {
        // auto-generated
    }
}
