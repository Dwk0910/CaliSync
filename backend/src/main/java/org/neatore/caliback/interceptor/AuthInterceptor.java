package org.neatore.caliback.interceptor;

import org.jetbrains.annotations.NotNull;

import lombok.RequiredArgsConstructor;

import org.neatore.caliback.object.MCPRequestClientInfo;
import org.neatore.caliback.services.UserVerifyService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;


@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {
    private final UserVerifyService uvs;
    private final MCPRequestClientInfo mcpci;

    @Override
    public boolean preHandle(HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull Object handler) {
        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String authorization = header.substring(7);
            if (uvs.verify(authorization)) {
                response.setStatus(HttpServletResponse.SC_OK);
                mcpci.setSessionToken(authorization);

                return true;
            }
        }

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setHeader("WWW-Authenticate",
                "Bearer realm=\"CaliSync\", " +
                        "resource_metadata_url=\"https://neatorebackend.kro.kr/calisync/.well-known/oauth-authorization-server\""
        );
        return false;
    }
}
