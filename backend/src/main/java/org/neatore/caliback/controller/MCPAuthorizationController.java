package org.neatore.caliback.controller;

import lombok.RequiredArgsConstructor;

import org.neatore.caliback.services.UserVerifyService;
import org.neatore.caliback.services.AuthorizationService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import org.springframework.web.servlet.view.RedirectView;

import java.net.URI;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class MCPAuthorizationController {
    private final UserVerifyService uvs;
    private final AuthorizationService authorizationService;

    @GetMapping("/authorize")
    public RedirectView authorize(@RequestParam String response_type, @RequestParam String client_id, @RequestParam String redirect_uri, @RequestParam String state, HttpServletRequest request) {
        HttpSession session = request.getSession();
        session.setAttribute("mcp_redirect_uri_n", request.getRequestURL() + "/next");
        session.setAttribute("mcp_redirect_uri_f", redirect_uri);
        session.setAttribute("mcp_state", state);

        return new RedirectView("https://accounts.google.com/o/oauth2/v2/auth" +
                "?client_id=" + client_id
                + "&redirect_uri=" + request.getRequestURL() + "/next"
                + "&response_type=" + response_type
                + "&scope=email"
                + "&state=" + state
        );
    }

    @GetMapping("/authorize/next")
    public ResponseEntity<Void> nextAuth(HttpServletRequest request, @RequestParam String code) {
        HttpSession session = request.getSession();

        if (authorizationService.verify(code, session.getAttribute("mcp_redirect_uri_n").toString())) {
            return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(
                    session.getAttribute("mcp_redirect_uri_f").toString()
                            + "?code=" + uvs.addSession()
                            + "&state=" + session.getAttribute("mcp_state")
            )).build();

        } return ResponseEntity.status(401).build();
    }

    @PostMapping("/token")
    public ResponseEntity<Map<String, Object>> exchangeToken(@RequestParam String code) {
        if (!uvs.verify(code)) return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();

        Map<String, Object> response = new HashMap<>();
        response.put("access_token", code);
        response.put("token_type", "Bearer");
        response.put("expires_in", 3600);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/.well-known/oauth-authorization-server")
    public Map<String, Object> oauthMetadata(HttpServletRequest request) {
        String baseUrl = request.getRequestURL().toString().replace(request.getRequestURI(), "");

        return Map.of(
                "issuer", baseUrl + "/calimcp/sse",
                "authorization_endpoint", baseUrl + "/authorize",
                "token_endpoint", baseUrl + "/token",
                "response_types_supported", List.of("code"),
                "grant_types_supported", List.of("authorization_code"),
                "token_endpoint_auth_methods_supported", List.of("client_secret_post", "client_secret_basic")
        );
    }
}
