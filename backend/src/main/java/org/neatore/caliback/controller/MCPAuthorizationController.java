package org.neatore.caliback.controller;

import lombok.RequiredArgsConstructor;

import org.neatore.caliback.CaliBack;
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
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class MCPAuthorizationController {
    // MCP Auth 순서
    // /.well-known -> /authorize -> (Google OAuth2) -> /authorize/next?code=xxxxxxx -> (처음 /authorize할 때 넘겨받은 redirect_uri로 이동) -> /token

    private final UserVerifyService uvs;
    private final AuthorizationService authorizationService;

    private static final String oauth_client_id = System.getenv("GOOGLEOAUTH_CLIENT_ID");

    private String getRequestURL(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String url = request.getRequestURL().toString().replace(uri, "");

        if (url.startsWith("http://localhost:8080") || url.endsWith("ngrok-free.dev")) return url;
        return url + "/calisync";
    }

    @GetMapping("/authorize")
    public RedirectView authorize(HttpServletRequest request, @RequestParam String redirect_uri, @RequestParam String state) {
        HttpSession session = request.getSession();
        session.setAttribute("mcp_redirect_uri_n", getRequestURL(request) + "/authorize/next");
        session.setAttribute("mcp_redirect_uri_f", redirect_uri);
        session.setAttribute("mcp_state", state);

        return new RedirectView("https://accounts.google.com/o/oauth2/v2/auth" +
                "?client_id=" + oauth_client_id
                + "&redirect_uri=" + getRequestURL(request) + "/authorize/next"
                + "&response_type=code"
                + "&scope=email"
                + "&state=" + state
        );
    }

    @GetMapping("/authorize/manual")
    public ResponseEntity<String> authorizeManual(HttpServletRequest request, @RequestParam(required = false) String code, @RequestParam(required = false) String state) {
        HttpSession session = request.getSession();

        if (code == null) {
            // Step 1. 사용자를 Google OAuth2 서비스 페이지로 이동
            String state_ = UUID.randomUUID().toString();
            String redirect_uri = getRequestURL(request) + "/authorize/manual";
            session.setAttribute("mcp_state", state_);
            session.setAttribute("mcp_redirect_uri_n", redirect_uri);

            return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(
                    "https://accounts.google.com/o/oauth2/v2/auth?client_id=" + oauth_client_id
                    + "&redirect_uri=" + redirect_uri
                    + "&response_type=code"
                    + "&scope=email"
                    + "&state=" + state_
            )).build();
        } else if (state != null && session.getAttribute("mcp_state").equals(state) && authorizationService.verify(code, session.getAttribute("mcp_redirect_uri_n").toString())) {
            // Step 2. 받은 authorization code를 통해 사용자 인증
            return ResponseEntity.ok("User verified. Generated session ID : " + uvs.addSession());
        } else return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }

    @GetMapping("/authorize/next")
    public ResponseEntity<Void> nextAuth(HttpServletRequest request, @RequestParam(required = false) String code, @RequestParam(required = false) String error) {
        if (code == null) CaliBack.LOGGER.warn("Authorization code is null. MCP Server will not be registered.");

        if (error != null) {
            CaliBack.LOGGER.error(error);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

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

    @PostMapping("/calimcp/sse")
    public ResponseEntity<Void> handleSsePost() {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).build();
    }

    @GetMapping("/.well-known/oauth-authorization-server")
    public Map<String, Object> oauthMetadata(HttpServletRequest request) {
        String url = getRequestURL(request);

        return Map.of(
                "issuer", url + "/calimcp/sse",
                "authorization_endpoint", url + "/authorize",
                "token_endpoint", url + "/token",
                "response_types_supported", List.of("code"),
                "grant_types_supported", List.of("authorization_code"),
                "token_endpoint_auth_methods_supported", List.of("client_secret_post", "client_secret_basic")
        );
    }

    @GetMapping("/.well-known/oauth-protected-resource/**")
    public ResponseEntity<Void> oauthProtectedResource() {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }
}
