package com.team3.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

class KakaoClientTests {
    private final RestClient.Builder builder = RestClient.builder();
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
    private final KakaoClient client = new KakaoClient(builder.build(), "app", "secret",
        "https://api.test/auth/kakao/callback");

    @Test
    void exchangesEncodedCodeAndUsesBearerTokenToGetIdentity() {
        assertThat(client.authorizationUri("state").toString()).contains("response_type=code", "state=state",
            "client_id=app");
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", "app");
        form.add("client_secret", "secret");
        form.add("redirect_uri", "https://api.test/auth/kakao/callback");
        form.add("code", "code+&=");
        server.expect(requestTo("https://kauth.kakao.com/oauth/token")).andExpect(method(HttpMethod.POST))
            .andExpect(content().formData(form)).andRespond(
                withSuccess("{\"access_token\":\"provider-token\",\"expires_in\":100}", MediaType.APPLICATION_JSON));
        server.expect(requestTo("https://kapi.kakao.com/v2/user/me")).andExpect(method(HttpMethod.GET))
            .andExpect(header("Authorization", "Bearer provider-token"))
            .andRespond(withSuccess("{\"id\":123,\"kakao_account\":{}}", MediaType.APPLICATION_JSON));
        assertThat(client.userId("code+&=")).isEqualTo(123L);
        server.verify();
    }

    @Test
    void rejectsProviderErrorsAndMalformedResponsesWithoutLeakingBody() {
        for (HttpStatus status : new HttpStatus[]{HttpStatus.BAD_REQUEST, HttpStatus.UNAUTHORIZED,
                HttpStatus.TOO_MANY_REQUESTS, HttpStatus.INTERNAL_SERVER_ERROR}) {
            server.reset();
            server.expect(requestTo("https://kauth.kakao.com/oauth/token"))
                .andRespond(withStatus(status).body("provider-secret"));
            assertFailure(status == HttpStatus.BAD_REQUEST || status == HttpStatus.UNAUTHORIZED
                ? HttpStatus.UNAUTHORIZED
                : HttpStatus.BAD_GATEWAY);
        }
        for (String body : new String[]{"{}", "null", "broken"}) {
            server.reset();
            server.expect(requestTo("https://kauth.kakao.com/oauth/token"))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
            assertFailure(HttpStatus.BAD_GATEWAY);
        }
        for (String body : new String[]{"{}", "{\"id\":0}", "null"}) {
            server.reset();
            server.expect(requestTo("https://kauth.kakao.com/oauth/token"))
                .andRespond(withSuccess("{\"access_token\":\"token\"}", MediaType.APPLICATION_JSON));
            server.expect(requestTo("https://kapi.kakao.com/v2/user/me"))
                .andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
            assertFailure(HttpStatus.BAD_GATEWAY);
        }
    }

    private void assertFailure(HttpStatus status) {
        assertThatThrownBy(() -> client.userId("code")).isInstanceOfSatisfying(ResponseStatusException.class, ex -> {
            assertThat(ex.getStatusCode()).isEqualTo(status);
            assertThat(ex.getMessage()).doesNotContain("provider-secret");
            assertThat(ex.getCause()).isNull();
        });
        server.verify();
    }
}
