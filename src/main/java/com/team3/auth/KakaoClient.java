package com.team3.auth;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

@Component
@ConditionalOnProperty(name = "auth.kakao.enabled", havingValue = "true")
public class KakaoClient {
    private final RestClient client;
    private final String clientId;
    private final String clientSecret;
    private final String adminKey;

    @Autowired
    public KakaoClient(RestClient.Builder builder, KakaoProperties properties) {
        this(createClient(builder), properties);
    }

    KakaoClient(RestClient client, KakaoProperties properties) {
        String clientId = properties.clientId();
        if (clientId.isBlank() || properties.adminKey().isBlank()) {
            throw new IllegalArgumentException("Valid Kakao settings are required.");
        }
        this.client = client;
        this.clientId = clientId;
        this.clientSecret = properties.clientSecret();
        this.adminKey = properties.adminKey();
    }

    private static RestClient createClient(RestClient.Builder builder) {
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5)).followRedirects(HttpClient.Redirect.NEVER).build());
        factory.setReadTimeout(Duration.ofSeconds(5));
        return builder.requestFactory(factory).build();
    }

    public UserInfo userInfo(String code, String redirectUri) {
        try {
            URI redirect = URI.create(redirectUri);
            if (redirect.getHost() == null || redirect.getFragment() != null
                || !("https".equals(redirect.getScheme()) || "http".equals(redirect.getScheme()))) {
                throw new IllegalArgumentException();
            }
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid redirectUri.");
        }
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", clientId);
        form.add("redirect_uri", redirectUri);
        form.add("code", code);
        if (!clientSecret.isBlank()) {
            form.add("client_secret", clientSecret);
        }
        try {
            Token token = client.post().uri("https://kauth.kakao.com/oauth/token")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED).body(form).retrieve().body(Token.class);
            if (token == null || token.accessToken() == null || token.accessToken().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Invalid Kakao response.");
            }
            User user = client.get().uri("https://kapi.kakao.com/v2/user/me?secure_resource=true")
                .headers(headers -> headers.setBearerAuth(token.accessToken())).retrieve().body(User.class);
            if (user == null || user.id() == null || user.id() <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Invalid Kakao response.");
            }
            Profile profile = user.account() == null ? null : user.account().profile();
            return new UserInfo(user.id(), profile == null ? null : profile.profileImageUrl());
        } catch (RestClientResponseException ex) {
            HttpStatus status = ex.getStatusCode().value() == 400 || ex.getStatusCode().value() == 401
                ? HttpStatus.UNAUTHORIZED
                : HttpStatus.BAD_GATEWAY;
            throw new ResponseStatusException(status, "Kakao login failed.");
        } catch (RestClientException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Kakao login unavailable.");
        }
    }

    public void unlink(String providerId) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("target_id_type", "user_id");
        form.add("target_id", providerId);
        try {
            User user = client.post().uri("https://kapi.kakao.com/v1/user/unlink")
                .headers(headers -> headers.set("Authorization", "KakaoAK " + adminKey))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED).body(form).retrieve().body(User.class);
            if (user == null || user.id() == null || !Long.toString(user.id()).equals(providerId)) {
                throw unlinkFailure();
            }
        } catch (RestClientResponseException ex) {
            if (alreadyUnlinked(ex)) {
                return;
            }
            throw unlinkFailure();
        } catch (RestClientException ex) {
            throw unlinkFailure();
        }
    }

    private ResponseStatusException unlinkFailure() {
        return new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Kakao unlink failed.");
    }

    private boolean alreadyUnlinked(RestClientResponseException ex) {
        try {
            KakaoError error = ex.getResponseBodyAs(KakaoError.class);
            return error != null && Integer.valueOf(-101).equals(error.code());
        } catch (RestClientException ignored) {
            return false;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Token(@JsonProperty("access_token") String accessToken) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record User(Long id, @JsonProperty("kakao_account") Account account) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Account(Profile profile) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Profile(@JsonProperty("profile_image_url") String profileImageUrl) {
    }

    public record UserInfo(long id, String profileImageUrl) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record KakaoError(Integer code) {
    }

}
