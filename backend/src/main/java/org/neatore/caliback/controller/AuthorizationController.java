package org.neatore.caliback.controller;

import lombok.RequiredArgsConstructor;

import org.neatore.caliback.services.AuthorizationService;
import org.neatore.caliback.services.UserVerifyService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthorizationController {
    private final UserVerifyService uvs;
    private final AuthorizationService authorizationService;

    @PostMapping("/getToken")
    public ResponseEntity<String> getToken(@RequestBody Map<String, Object> body) {
        String auth_code = body.get("auth_code").toString();
        String redirectUri = body.get("redirect_uri").toString();

        if (authorizationService.verify(auth_code, redirectUri)) return ResponseEntity.ok(uvs.addSession());

        // 인증되지 않은 사용자
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verify(@RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(uvs.verify(body.get("token").toString()));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestBody Map<String, Object> body) {
        uvs.deleteSession(body.get("token").toString());
        return ResponseEntity.ok().build();
    }
}
