package com.team3.auth;

import com.team3.user.User;
import com.team3.user.Provider;
import com.team3.user.UserAgreementRepository;
import com.team3.user.UserRepository;
import com.team3.whisky.PriceHistoryRepository;
import com.team3.whisky.SaleProductRepository;
import com.team3.whisky.WhiskyCategoryRepository;
import com.team3.collection.CollectionRepository;
import com.team3.collection.CollectionWhiskyRepository;
import com.team3.planner.PlannerItemRepository;
import com.team3.planner.PlannerRepository;
import com.team3.exchange.ExchangeRateRepository;
import com.team3.curation.CurationRepository;
import com.team3.curation.CurationWhiskyRepository;
import com.team3.whisky.WhiskyOriginRepository;
import com.team3.whisky.WhiskyRegionRepository;
import com.team3.whisky.WhiskyRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Optional;
import static org.mockito.Mockito.mock;

import com.team3.auth.token.RefreshTokenRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
            + "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration",
        "auth.jwt.secret=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=", "auth.kakao.enabled=true",
        "auth.kakao.admin-key=admin"
})
@AutoConfigureMockMvc
class KakaoHttpTests {
    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper json;
    @Autowired
    private JwtDecoder decoder;
    @Autowired
    private AuthService tokens;
    @MockitoBean
    private KakaoClient kakao;
    @MockitoBean
    private UserRepository users;
    @MockitoBean
    private RefreshTokenRepository refreshTokens;
    @MockitoBean
    private UserAgreementRepository agreements;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMappingContext;

    @MockitoBean
    private PlatformTransactionManager transactionManager;

    @MockitoBean
    private WhiskyCategoryRepository whiskyCategories;
    @MockitoBean
    private WhiskyRepository whiskies;

    @MockitoBean
    private CollectionRepository collections;
    @MockitoBean
    private PriceHistoryRepository priceHistories;
    @MockitoBean
    private SaleProductRepository saleProducts;
    @MockitoBean
    private WhiskyOriginRepository whiskyOrigins;
    @MockitoBean
    private WhiskyRegionRepository whiskyRegions;

    @MockitoBean
    private CollectionWhiskyRepository collectionWhiskies;

    @MockitoBean
    private PlannerRepository planners;

    @MockitoBean
    private PlannerItemRepository plannerItems;

    @MockitoBean
    private ExchangeRateRepository exchangeRates;

    @MockitoBean
    private CurationRepository curationRepository;

    @MockitoBean
    private CurationWhiskyRepository curationWhiskies;

    @Test
    void exchangesCodeWithoutSessionOrCookies() throws Exception {
        Long id = 42L;
        when(kakao.userId("code", "https://frontend.test/callback")).thenReturn(123L);
        User user = mock(User.class);
        when(user.id()).thenReturn(id);
        when(users.findByProviderAndProviderId(Provider.KAKAO, "123")).thenReturn(Optional.of(user));
        when(users.findLockedById(id)).thenReturn(Optional.of(user));
        MvcResult result = mvc
            .perform(post("/api/v1/auth/kakao").queryParam("redirectUri", "https://frontend.test/callback")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"code\"}"))
            .andExpect(status().isOk())
            .andExpect(header().string("Cache-Control", "no-cache, no-store, max-age=0, must-revalidate"))
            .andExpect(header().string("Referrer-Policy", "no-referrer"))
            .andExpect(jsonPath("$.data.isNewUser").value(false))
            .andExpect(jsonPath("$.data.refreshToken").isString())
            .andExpect(jsonPath("$.data.expiresIn").doesNotExist())
            .andReturn();
        String access = json.readTree(result.getResponse().getContentAsString()).get("data").get("accessToken")
            .asText();
        assertThat(decoder.decode(access).getSubject()).isEqualTo(id.toString());
        assertThat(result.getRequest().getSession(false)).isNull();
        assertThat(result.getResponse().getHeader("Set-Cookie")).isNull();
        verify(users, never()).saveAndFlush(any(User.class));
    }

    @Test
    void returnsNewUserWhenRegistrationSucceeds() throws Exception {
        when(kakao.userId("code", "https://frontend.test/callback")).thenReturn(123L);
        User user = mock(User.class);
        when(user.id()).thenReturn(42L);
        when(user.isPending()).thenReturn(true);
        when(users.saveAndFlush(any(User.class))).thenReturn(user);
        when(users.findLockedById(42L)).thenReturn(Optional.of(user));
        MvcResult result = mvc
            .perform(post("/api/v1/auth/kakao").queryParam("redirectUri", "https://frontend.test/callback")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"code\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.isNewUser").value(true))
            .andExpect(jsonPath("$.data.refreshToken").isString())
            .andReturn();
        String access = json.readTree(result.getResponse().getContentAsString()).get("data").get("accessToken")
            .asText();
        assertThat(decoder.decode(access).getSubject()).isEqualTo("42");
        verify(users).saveAndFlush(any(User.class));
    }

    @Test
    void rejectsMissingBlankOversizedAndMalformedCode() throws Exception {
        for (String body : new String[]{"{}", "{\"code\":null}", "{\"code\":\" \"}",
                "{\"code\":\"" + "a".repeat(2049) + "\"}"}) {
            mvc.perform(post("/api/v1/auth/kakao").queryParam("redirectUri", "https://frontend.test/callback")
                .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors[0].field").value("code"))
                .andExpect(jsonPath("$.errors[0].message").isString())
                .andExpect(jsonPath("$.errors[0].rejectedValue").doesNotExist());
        }
        mvc.perform(post("/api/v1/auth/kakao").queryParam("redirectUri", "https://frontend.test/callback")
            .contentType(MediaType.APPLICATION_JSON).content("{"))
            .andExpect(status().isBadRequest());
        verifyNoInteractions(kakao, users, refreshTokens);
    }

    @Test
    void preservesProviderFailureStatusWithoutIssuingTokens() throws Exception {
        for (HttpStatus status : new HttpStatus[]{HttpStatus.UNAUTHORIZED, HttpStatus.BAD_GATEWAY}) {
            doThrow(new ResponseStatusException(status, "Kakao login failed.")).when(kakao).userId("code",
                "https://frontend.test/callback");
            mvc.perform(post("/api/v1/auth/kakao").queryParam("redirectUri", "https://frontend.test/callback")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"code\"}"))
                .andExpect(status().is(status.value()))
                .andExpect(header().doesNotExist("Set-Cookie"));
        }
        verifyNoInteractions(users, refreshTokens);
    }

    @Test
    void noLongerExposesBrowserLoginOrCallback() throws Exception {
        mvc.perform(get("/api/v1/auth/kakao")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/auth/kakao/callback").param("code", "code"))
            .andExpect(status().isUnauthorized());
        verifyNoInteractions(kakao, users, refreshTokens);
    }

    @Test
    void withdrawsAuthenticatedUserAndReturnsNoContentOnRepeat() throws Exception {
        User user = new User(Provider.KAKAO, "123");
        when(users.findById(1L)).thenReturn(Optional.of(user));
        when(users.findLockedById(1L)).thenReturn(Optional.of(user));
        String accessToken = tokens.issue(1L).accessToken();

        mvc.perform(delete("/api/v1/auth/withdrawal").header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isNoContent());
        mvc.perform(delete("/api/v1/auth/withdrawal").header("Authorization", "Bearer " + accessToken))
            .andExpect(status().isNoContent());
        mvc.perform(delete("/api/v1/auth/withdrawal")).andExpect(status().isUnauthorized());
        verify(kakao).unlink("123");
    }
}
