package com.team3.auth;

import com.team3.user.User;
import com.team3.user.Provider;
import com.team3.user.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import static org.mockito.Mockito.mock;

import com.team3.auth.token.RefreshTokenRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
            + "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration",
        "auth.jwt.secret=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=", "auth.kakao.enabled=true"
})
@AutoConfigureMockMvc
class KakaoHttpTests {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper json;
    @Autowired
    private JwtDecoder decoder;
    @MockitoBean
    private KakaoClient kakao;
    @MockitoBean
    private UserRepository users;
    @MockitoBean
    private RefreshTokenRepository refreshTokens;
    @MockitoBean
    private Clock clock;

    @Test
    void logsInWithInternalSubjectAndRejectsCallbackReplay() throws Exception {
        MockHttpSession session = start();
        String state = state(session);
        Long id = 42L;
        when(kakao.userId("code")).thenReturn(123L);
        User user = mock(User.class);
        when(user.id()).thenReturn(id);
        when(users.findByProviderAndProviderId(Provider.KAKAO, "123")).thenReturn(Optional.of(user));
        MvcResult result = mvc.perform(get("/auth/kakao/callback").session(session)
            .param("state", state).param("code", "code"))
            .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
            .andExpect(header().string("Referrer-Policy", "no-referrer"))
            .andExpect(jsonPath("$.refreshToken").isString()).andExpect(jsonPath("$.expiresIn").value(900))
            .andReturn();
        String access = json.readTree(result.getResponse().getContentAsString()).get("accessToken").asText();
        assertThat(decoder.decode(access).getSubject()).isEqualTo(id.toString());
        mvc.perform(get("/auth/kakao/callback").session(session).param("state", state).param("code", "code"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsMissingWrongExpiredStateAndDeniedOrMissingCode() throws Exception {
        mvc.perform(get("/auth/kakao/callback").param("state", "unknown").param("code", "code"))
            .andExpect(status().isBadRequest());
        MockHttpSession wrong = start();
        mvc.perform(get("/auth/kakao/callback").session(wrong).param("state", "wrong").param("code", "code"))
            .andExpect(status().isBadRequest());
        MockHttpSession expired = start();
        String expiredState = state(expired);
        when(clock.instant()).thenReturn(Instant.now().plusSeconds(301));
        mvc.perform(get("/auth/kakao/callback").session(expired).param("state", expiredState).param("code", "code"))
            .andExpect(status().isBadRequest());
        MockHttpSession denied = start();
        mvc.perform(get("/auth/kakao/callback").session(denied).param("state", state(denied))
            .param("error", "access_denied").param("error_description", "sensitive"))
            .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.detail").value("Kakao login denied."));
        MockHttpSession missingCode = start();
        mvc.perform(get("/auth/kakao/callback").session(missingCode).param("state", state(missingCode)))
            .andExpect(status().isBadRequest());
        verifyNoInteractions(users, refreshTokens);
    }

    private MockHttpSession start() throws Exception {
        when(clock.instant()).thenReturn(Instant.now());
        when(kakao.authorizationUri(anyString())).thenAnswer(call -> URI.create("https://kauth.kakao.com/?state="
            + call.getArgument(0)));
        return (MockHttpSession) mvc.perform(get("/auth/kakao")).andExpect(status().isFound())
            .andExpect(header().string("Cache-Control", "no-store")).andReturn().getRequest().getSession(false);
    }

    private String state(MockHttpSession session) throws Exception {
        Object stored = session.getAttribute(AuthController.class.getName() + ".state");
        return json.valueToTree(stored).get("value").asText();
    }
}
