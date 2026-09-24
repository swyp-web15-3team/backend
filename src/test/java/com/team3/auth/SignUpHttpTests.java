package com.team3.auth;

import com.team3.user.enums.AgreementType;
import com.team3.user.enums.Provider;
import com.team3.user.User;
import com.team3.user.UserAgreement;
import com.team3.user.UserAgreementRepository;
import com.team3.user.UserRepository;
import com.team3.whisky.WhiskyCategoryRepository;
import com.team3.whisky.PriceHistoryRepository;
import com.team3.whisky.SaleProductRepository;
import com.team3.whisky.WhiskyOriginRepository;
import com.team3.whisky.WhiskyRegionRepository;
import com.team3.whisky.WhiskyRepository;
import com.team3.collection.CollectionRepository;
import com.team3.collection.CollectionWhiskyRepository;
import com.team3.planner.PlannerItemRepository;
import com.team3.planner.PlannerRepository;
import com.team3.exchange.ExchangeRateRepository;
import com.team3.curation.CurationRepository;
import com.team3.curation.CurationWhiskyRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Optional;

import com.team3.auth.token.RefreshTokenRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.mockito.ArgumentCaptor;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
            + "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration",
        "auth.jwt.secret=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
})
@AutoConfigureMockMvc
@MockitoBean(types = {JpaMetamodelMappingContext.class, CollectionRepository.class, CollectionWhiskyRepository.class,
        PlannerRepository.class, PlannerItemRepository.class, ExchangeRateRepository.class,
        CurationRepository.class, CurationWhiskyRepository.class,
        PriceHistoryRepository.class, SaleProductRepository.class, WhiskyOriginRepository.class,
        WhiskyRegionRepository.class, WhiskyRepository.class, PlatformTransactionManager.class})
class SignUpHttpTests {

    private static final String BODY = "{\"ageOver14Agreed\":true,\"termsOfServiceAgreed\":true,"
        + "\"privacyPolicyAgreed\":true,\"nickname\":\"tester\"}";

    @Autowired
    private MockMvc mvc;
    @Autowired
    private AuthService tokens;
    @MockitoBean
    private UserRepository users;
    @MockitoBean
    private UserAgreementRepository agreements;
    @MockitoBean
    private RefreshTokenRepository refreshTokens;
    @MockitoBean
    private WhiskyCategoryRepository whiskyCategories;

    @BeforeEach
    void setUp() {
        when(users.findLockedById(1L)).thenReturn(Optional.of(new User(Provider.KAKAO, "123")));
    }

    @Test
    void activatesPendingUserAndRecordsEveryAgreement() throws Exception {
        User user = new User(Provider.KAKAO, "123");
        when(users.findLockedById(1L)).thenReturn(Optional.of(user));
        mvc.perform(signUp(BODY)).andExpect(status().isNoContent()).andExpect(content().string(""));
        assertThat(user.isPending()).isFalse();
        assertThat(user.nickname()).isEqualTo("tester");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<UserAgreement>> saved = ArgumentCaptor.forClass(List.class);
        verify(agreements).saveAll(saved.capture());
        assertThat(saved.getValue()).extracting(UserAgreement::userId, UserAgreement::type, UserAgreement::agreed)
            .containsExactly(
                org.assertj.core.groups.Tuple.tuple(1L, AgreementType.AGE_OVER_14, true),
                org.assertj.core.groups.Tuple.tuple(1L, AgreementType.TERMS_OF_SERVICE, true),
                org.assertj.core.groups.Tuple.tuple(1L, AgreementType.PRIVACY_POLICY, true),
                org.assertj.core.groups.Tuple.tuple(1L, AgreementType.MARKETING, false));
    }

    @Test
    void rejectsSecondSignUpForActiveUser() throws Exception {
        User user = new User(Provider.KAKAO, "123");
        user.activate();
        when(users.findLockedById(1L)).thenReturn(Optional.of(user));
        mvc.perform(signUp(BODY))
            .andExpect(status().isConflict())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.code").value("AUTH_002"))
            .andExpect(jsonPath("$.detail").value("이미 가입이 완료된 사용자입니다."));
        verify(agreements, never()).saveAll(anyList());
    }

    @Test
    void rejectsMissingTokenAndRequiredAgreementFalseOrMissing() throws Exception {
        mvc.perform(post("/api/v1/auth/sign-up").contentType(MediaType.APPLICATION_JSON).content(BODY))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("AUTH_001"));

        for (String[] required : new String[][]{
                {"ageOver14Agreed", "만 14세 이상 동의가 필요합니다."},
                {"termsOfServiceAgreed", "이용약관 동의가 필요합니다."},
                {"privacyPolicyAgreed", "개인정보 수집 및 이용 동의가 필요합니다."}}) {
            for (String body : new String[]{agreementBody(null, required[0]), agreementBody(required[0], null)}) {
                mvc.perform(signUp(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.errors[0].field").value(required[0]))
                    .andExpect(jsonPath("$.errors[0].message").value(required[1]));
            }
        }
        verify(users, never()).findLockedById(1L);
    }

    @Test
    void rejectsInvalidNicknameOnSignUp() throws Exception {
        for (String field : new String[]{"", ",\"nickname\":null", ",\"nickname\":\" \"",
                ",\"nickname\":\"" + "a".repeat(31) + "\""}) {
            String body = BODY.replace(",\"nickname\":\"tester\"", field);
            mvc.perform(signUp(body)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("nickname"));
        }
        verify(users, never()).findLockedById(1L);
    }

    @Test
    void updatesNicknameAndRejectsInvalidValues() throws Exception {
        User user = new User(Provider.KAKAO, "123");
        org.springframework.test.util.ReflectionTestUtils.setField(user, "id", 1L);
        user.activate();
        user.updateNickname("before");
        when(users.findLockedById(1L)).thenReturn(Optional.of(user));
        when(users.findById(1L)).thenReturn(Optional.of(user));
        String bearer = "Bearer " + tokens.issue(1L, "https://img.test/photo.jpg").accessToken();
        String nickname = "가".repeat(30);
        mvc.perform(put("/api/v1/users/me/profile").header("Authorization", bearer)
            .contentType(MediaType.APPLICATION_JSON).content("{\"nickname\":\"" + nickname + "\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").value(1))
            .andExpect(jsonPath("$.data.nickname").value(nickname))
            .andExpect(jsonPath("$.data.profileImageUrl").value("https://img.test/photo.jpg"))
            .andExpect(jsonPath("$.data.providerId").doesNotExist());
        assertThat(user.nickname()).isEqualTo(nickname);
        for (String body : new String[]{"{}", "{\"nickname\":null}", "{\"nickname\":\" \"}",
                "{\"nickname\":\"" + "a".repeat(31) + "\"}"}) {
            mvc.perform(put("/api/v1/users/me/profile").header("Authorization", bearer)
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("nickname"));
        }
        assertThat(user.nickname()).isEqualTo(nickname);
        user.delete(java.time.Instant.now());
        assertThat(user.nickname()).isNull();
        mvc.perform(put("/api/v1/users/me/profile").header("Authorization", bearer)
            .contentType(MediaType.APPLICATION_JSON).content("{\"nickname\":\"after\"}"))
            .andExpect(status().isForbidden());
    }

    @Test
    void returnsUserErrorCodeWhenUserDisappearsBeforeUpdate() throws Exception {
        User user = new User(Provider.KAKAO, "123");
        user.activate();
        when(users.findLockedById(1L)).thenReturn(Optional.of(user));
        when(users.findById(1L)).thenReturn(Optional.of(user));
        String bearer = "Bearer " + tokens.issue(1L, null).accessToken();
        when(users.findLockedById(1L)).thenReturn(Optional.empty());
        mvc.perform(put("/api/v1/users/me/profile").header("Authorization", bearer)
            .contentType(MediaType.APPLICATION_JSON).content("{\"nickname\":\"tester\"}"))
            .andExpect(status().isUnauthorized())
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
            .andExpect(jsonPath("$.code").value("USER_001"))
            .andExpect(jsonPath("$.detail").value("사용자를 찾을 수 없습니다."));
    }

    @Test
    void getsCurrentUserAndRequiresAnActiveAccount() throws Exception {
        mvc.perform(get("/api/v1/users/me")).andExpect(status().isUnauthorized());
        User user = new User(Provider.KAKAO, "123");
        org.springframework.test.util.ReflectionTestUtils.setField(user, "id", 1L);
        when(users.findLockedById(1L)).thenReturn(Optional.of(user));
        when(users.findById(1L)).thenReturn(Optional.of(user));
        String bearer = "Bearer " + tokens.issue(1L, "https://img.test/photo.jpg").accessToken();
        mvc.perform(get("/api/v1/users/me").header("Authorization", bearer))
            .andExpect(status().isForbidden());
        user.activate();
        user.updateNickname("tester");
        clearInvocations(users);
        mvc.perform(get("/api/v1/users/me").header("Authorization", bearer))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").value(1))
            .andExpect(jsonPath("$.data.nickname").value("tester"))
            .andExpect(jsonPath("$.data.profileImageUrl").value("https://img.test/photo.jpg"))
            .andExpect(jsonPath("$.data.providerId").doesNotExist());
        verify(users, never()).findLockedById(1L);
        user.delete(java.time.Instant.now());
        mvc.perform(get("/api/v1/users/me").header("Authorization", bearer))
            .andExpect(status().isForbidden());
    }

    private String agreementBody(String omitted, String declined) {
        List<String> fields = new java.util.ArrayList<>();
        fields.add("\"nickname\":\"tester\"");
        addRequired(fields, "ageOver14Agreed", omitted, declined);
        addRequired(fields, "termsOfServiceAgreed", omitted, declined);
        addRequired(fields, "privacyPolicyAgreed", omitted, declined);
        return "{" + String.join(",", fields) + "}";
    }

    private void addRequired(List<String> fields, String field, String omitted, String declined) {
        if (!field.equals(omitted)) {
            fields.add("\"" + field + "\":" + !field.equals(declined));
        }
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder signUp(String body) {
        String accessToken = tokens.issue(1L, null).accessToken();
        clearInvocations(users);
        return post("/api/v1/auth/sign-up").contentType(MediaType.APPLICATION_JSON).content(body)
            .header("Authorization", "Bearer " + accessToken);
    }
}
