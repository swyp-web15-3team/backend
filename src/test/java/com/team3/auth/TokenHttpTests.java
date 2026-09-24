package com.team3.auth;

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

import com.team3.auth.token.RefreshToken;
import com.team3.auth.token.RefreshTokenRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
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

import com.team3.user.enums.Provider;
import com.team3.user.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
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

    @MockitoBean
    private UserAgreementRepository agreements;

    @MockitoBean
    private JpaMetamodelMappingContext jpaMappingContext;

    @MockitoBean
    private PlatformTransactionManager transactionManager;

    @MockitoBean
    private WhiskyCategoryRepository whiskyCategories;

    @MockitoBean
    private CollectionRepository collections;

    @MockitoBean
    private WhiskyRepository whiskies;

    @BeforeEach
    void setUp() {
        User user = new User(Provider.KAKAO, "123");
        user.activate();
        when(users.findById(1L)).thenReturn(Optional.of(user));
        when(users.findLockedById(1L)).thenReturn(Optional.of(user));
    }

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
    void disabledKakaoLoginDoesNotCreateSession() throws Exception {
        MvcResult result = mvc
            .perform(post("/api/v1/auth/kakao").queryParam("redirectUri", "https://frontend.test/callback")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\":\"code\"}"))
            .andExpect(status().isNotFound()).andReturn();
        assertThat(result.getRequest().getSession(false)).isNull();
        assertThat(result.getResponse().getHeader("Set-Cookie")).isNull();
    }

    @Test
    void protectedEndpointRequiresValidBearerToken() throws Exception {
        mvc.perform(get("/api/v1/test/me"))
            .andExpect(status().isUnauthorized())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, startsWith("Bearer")))
            .andExpect(jsonPath("$.title").value("Unauthorized"))
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.detail").value("인증이 필요합니다."))
            .andExpect(jsonPath("$.instance").value("/api/v1/test/me"))
            .andExpect(jsonPath("$.code").value("AUTH_001"));
        mvc.perform(get("/api/v1/test/me").header("Authorization", "Bearer invalid"))
            .andExpect(status().isUnauthorized())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, startsWith("Bearer")))
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.code").value("AUTH_001"));
        mvc.perform(get("/api/v1/test/me").header("Authorization", "Bearer " + tokens.issue(1L, null).accessToken()))
            .andExpect(status().isOk()).andExpect(content().string("1"));
    }

    @Test
    void refreshWorksWithoutAccessTokenAndDoesNotCacheTokens() throws Exception {
        when(users.findLockedById(1L)).thenReturn(Optional.of(new User(Provider.KAKAO, "pending")));
        when(repository.findOwnerByTokenHash(anyString())).thenReturn(Optional.of(() -> 1L));
        when(repository.findByTokenHash(anyString())).thenReturn(Optional.of(
            new RefreshToken(1L, "hash", Instant.now().plusSeconds(3600))));
        mvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
            .content("{\"refreshToken\":\"" + "a".repeat(43) + "\"}"))
            .andExpect(status().isOk())
            .andExpect(header().string("Cache-Control", "no-cache, no-store, max-age=0, must-revalidate"))
            .andExpect(jsonPath("$.accessToken").doesNotExist())
            .andExpect(jsonPath("$.data.accessToken").isString())
            .andExpect(jsonPath("$.data.refreshToken").isString())
            .andExpect(jsonPath("$.data.expiresIn").doesNotExist());
    }

    @Test
    void sameTokenRequiresSignUpAndStopsWorkingAfterWithdrawalOrUserRemoval() throws Exception {
        User user = new User(Provider.KAKAO, "pending");
        when(users.findById(1L)).thenReturn(Optional.of(user));
        when(users.findLockedById(1L)).thenReturn(Optional.of(user));
        String bearer = "Bearer " + tokens.issue(1L, null).accessToken();

        mvc.perform(get("/api/v1/test/me").header(HttpHeaders.AUTHORIZATION, bearer))
            .andExpect(status().isForbidden())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.code").value("AUTH_005"))
            .andExpect(jsonPath("$.status").value(403))
            .andExpect(jsonPath("$.instance").value("/api/v1/test/me"));
        mvc.perform(get("/actuator/health").header(HttpHeaders.AUTHORIZATION, bearer))
            .andExpect(status().isOk());
        mvc.perform(post("/api/v1/auth/sign-up").header(HttpHeaders.AUTHORIZATION, bearer)
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                "{\"ageOver14Agreed\":true,\"termsOfServiceAgreed\":true,\"privacyPolicyAgreed\":true,\"nickname\":\"tester\"}"))
            .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/test/me").header(HttpHeaders.AUTHORIZATION, bearer))
            .andExpect(status().isOk()).andExpect(content().string("1"));

        user.delete(Instant.now());
        mvc.perform(get("/api/v1/test/me").header(HttpHeaders.AUTHORIZATION, bearer))
            .andExpect(status().isForbidden());
        when(users.findById(1L)).thenReturn(Optional.empty());
        mvc.perform(get("/api/v1/test/me").header(HttpHeaders.AUTHORIZATION, bearer))
            .andExpect(status().isForbidden());
    }

    @Test
    void invalidRequestsReturnClientErrorsAndLogoutIsIdempotent() throws Exception {
        mvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.data").doesNotExist());
        String body = "{\"refreshToken\":\"" + "a".repeat(43) + "\"}";
        mvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/auth/logout").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isNoContent()).andExpect(content().string(""));
    }

    @RestController
    static class ProtectedEndpoint {
        @GetMapping("/api/v1/test/me")
        String me(Authentication authentication) {
            return authentication.getName();
        }
    }
}
