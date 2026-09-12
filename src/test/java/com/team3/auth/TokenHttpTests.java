package com.team3.auth;

import com.team3.user.UserRepository;

import com.team3.auth.token.RefreshToken;
import com.team3.auth.token.RefreshTokenRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
            + "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration",
        "auth.jwt.secret=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
})
@AutoConfigureMockMvc
@Import(TokenHttpTests.ProtectedEndpoint.class)
class TokenHttpTests {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private AuthService tokens;
    @MockitoBean
    private RefreshTokenRepository repository;

    @MockitoBean
    private UserRepository users;

    @Test
    void disabledKakaoLoginDoesNotCreateSession() throws Exception {
        MvcResult result = mvc.perform(post("/auth/kakao").contentType(MediaType.APPLICATION_JSON)
            .content("{\"code\":\"code\"}"))
            .andExpect(status().isNotFound()).andReturn();
        assertThat(result.getRequest().getSession(false)).isNull();
        assertThat(result.getResponse().getHeader("Set-Cookie")).isNull();
    }

    @Test
    void protectedEndpointRequiresValidBearerToken() throws Exception {
        mvc.perform(get("/test/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/test/me").header("Authorization", "Bearer invalid"))
            .andExpect(status().isUnauthorized());
        mvc.perform(get("/test/me").header("Authorization", "Bearer " + tokens.issue(1L).accessToken()))
            .andExpect(status().isOk()).andExpect(content().string("1"));
    }

    @Test
    void refreshWorksWithoutAccessTokenAndDoesNotCacheTokens() throws Exception {
        when(repository.findByTokenHash(anyString())).thenReturn(Optional.of(
            new RefreshToken(1L, "hash", Instant.now().plusSeconds(3600))));
        mvc.perform(post("/auth/refresh").contentType(MediaType.APPLICATION_JSON)
            .content("{\"refreshToken\":\"" + "a".repeat(43) + "\"}"))
            .andExpect(status().isOk())
            .andExpect(header().string("Cache-Control", "no-cache, no-store, max-age=0, must-revalidate"))
            .andExpect(jsonPath("$.accessToken").doesNotExist())
            .andExpect(jsonPath("$.data.accessToken").isString())
            .andExpect(jsonPath("$.data.refreshToken").isString())
            .andExpect(jsonPath("$.data.expiresIn").doesNotExist());
    }

    @Test
    void invalidRequestsReturnClientErrorsAndLogoutIsIdempotent() throws Exception {
        mvc.perform(post("/auth/refresh").contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.data").doesNotExist());
        String body = "{\"refreshToken\":\"" + "a".repeat(43) + "\"}";
        mvc.perform(post("/auth/refresh").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isUnauthorized());
        mvc.perform(post("/auth/logout").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isNoContent()).andExpect(content().string(""));
    }

    @RestController
    static class ProtectedEndpoint {
        @GetMapping("/test/me")
        String me(Authentication authentication) {
            return authentication.getName();
        }
    }
}
