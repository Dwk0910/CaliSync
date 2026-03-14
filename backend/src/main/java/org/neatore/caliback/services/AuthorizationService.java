package org.neatore.caliback.services;

import org.json.JSONObject;

import org.neatore.caliback.CaliBack;

import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeTokenRequest;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.javanet.NetHttpTransport;

import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class AuthorizationService {
    public boolean verify(String authorization_code, String redirect_uri) {
        // 들어오는 Authorization Code를 통해 Google Access Token 구하기

        String clientId = System.getenv("GOOGLEOAUTH_CLIENT_ID");
        String secret = System.getenv("GOOGLEOAUTH_CLIENT_SECRET");

        if (clientId == null || secret == null) throw new RuntimeException("Google OAuth Client ID/SECRET not set.");

        try {
            TokenResponse tokenResponse = new GoogleAuthorizationCodeTokenRequest(
                    new NetHttpTransport(),
                    new GsonFactory(),
                    "https://oauth2.googleapis.com/token",
                    clientId, secret, authorization_code, redirect_uri
            ).execute();

            String google_token = tokenResponse.getAccessToken();

            // 구한 Google Access Token을 사용하여 유저 이메일 검증 및 세션 등록
            HttpRequestFactory requestFactory = new NetHttpTransport().createRequestFactory();
            GenericUrl url = new GenericUrl("https://www.googleapis.com/oauth2/v3/userinfo");

            HttpRequest request = requestFactory.buildGetRequest(url);
            request.getHeaders().setAuthorization("Bearer " + google_token);
            HttpResponse response_ = request.execute();
            JSONObject response = new JSONObject(response_.parseAsString());
            if (response.getString("email").equals(CaliBack.allowedEmail)) return true;
        } catch (IOException e) {
            CaliBack.LOGGER.error("", e);
        }

        return false;
    }
}
