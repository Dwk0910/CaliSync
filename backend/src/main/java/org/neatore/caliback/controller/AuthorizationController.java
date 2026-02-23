package org.neatore.caliback.controller;

import org.neatore.caliback.CaliBack;
import org.neatore.caliback.services.UserVerifyService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.javanet.NetHttpTransport;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthorizationController {
    private final UserVerifyService uvs;

    public AuthorizationController(UserVerifyService uvs) {
        this.uvs = uvs;
    }

    @PostMapping("/getToken")
    public ResponseEntity<String> getToken(@RequestBody Map<String, Object> body) {
        HttpRequestFactory requestFactory = new NetHttpTransport().createRequestFactory();
        GenericUrl url = new GenericUrl("https://www.googleapis.com/oauth2/v3/userinfo");

        try {
            HttpRequest request = requestFactory.buildGetRequest(url);
            request.getHeaders().setAuthorization("Bearer " + body.get("google_token"));
            HttpResponse response_ = request.execute();
            JSONObject response = new JSONObject(response_.parseAsString());
            if (response.getString("email").equals(CaliBack.allowedEmail)) return ResponseEntity.ok(uvs.addSession());
        } catch (IOException | JSONException e) {
            CaliBack.LOGGER.error("", e);
        }
        // 인증되지 않은 사용자
        return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
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
