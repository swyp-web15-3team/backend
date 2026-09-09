package com.team3.auth;

import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import org.springframework.web.util.UriComponentsBuilder;

@Component
@ConditionalOnProperty(name = "auth.kakao.enabled", havingValue = "true")
public class KakaoClient {
    private final RestClient client;
    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;

    @Autowired
    public KakaoClient(RestClient.Builder builder, @Value("${auth.kakao.client-id}") String clientId,
        @Value("${auth.kakao.client-secret}") String clientSecret,
        @Value("${auth.kakao.redirect-uri}") String redirectUri) {
        this(createClient(builder), clientId, clientSecret, redirectUri);
    }

    KakaoClient(RestClient client, String clientId, String clientSecret, String redirectUri) {
        URI redirect = URI.create(redirectUri);
        if (clientId.isBlank() || redirect.getHost() == null || redirect.getFragment() != null
            || !("https".equals(redirect.getScheme()) || "http".equals(redirect.getScheme()))) {
            throw new IllegalArgumentException("Valid Kakao client ID and redirect URI are required.");
        }
        this.client = client;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
    }

    private static RestClient createClient(RestClient.Builder builder) {
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5)).followRedirects(HttpClient.Redirect.NEVER).build());
        factory.setReadTimeout(Duration.ofSeconds(5));
        return builder.requestFactory(factory).build();
    }

    public URI authorizationUri(String state) {
        return UriComponentsBuilder.fromUriString("https://kauth.kakao.com/oauth/authorize")
            .queryParam("response_type", "code").queryParam("client_id", clientId)
            .queryParam("redirect_uri", redirectUri).queryParam("state", state).build().encode().toUri();
    }

    public long userId(String code) {
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
            User user = client.get().uri("https://kapi.kakao.com/v2/user/me")
                .headers(headers -> headers.setBearerAuth(token.accessToken())).retrieve().body(User.class);
            if (user == null || user.id() == null || user.id() <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Invalid Kakao response.");
            }
            return user.id();
        } catch (RestClientResponseException ex) {
            HttpStatus status = ex.getStatusCode().value() == 400 || ex.getStatusCode().value() == 401
                ? HttpStatus.UNAUTHORIZED
                : HttpStatus.BAD_GATEWAY;
            throw new ResponseStatusException(status, "Kakao login failed.");
        } catch (RestClientException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Kakao login unavailable.");
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Token(@JsonProperty("access_token") String accessToken) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record User(Long id) {
    }
}
