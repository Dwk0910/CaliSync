package org.neatore.caliback.interceptor;

import org.jetbrains.annotations.NotNull;

import lombok.RequiredArgsConstructor;

import org.neatore.caliback.services.UserVerifyService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;


@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {
    private final UserVerifyService uvs;

    @Override
    public boolean preHandle(HttpServletRequest request, @NotNull HttpServletResponse response, @NotNull Object handler) {
        String authorization = request.getHeader("Authorization").substring(7);
        if (uvs.verify(authorization)) return true;
        else {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }
    }
}
