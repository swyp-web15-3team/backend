package com.team3.auth;

import com.team3.user.AgreementType;
import com.team3.user.Provider;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
        PriceHistoryRepository.class, SaleProductRepository.class, WhiskyOriginRepository.class,
        WhiskyRegionRepository.class, WhiskyRepository.class, PlatformTransactionManager.class})
class SignUpHttpTests {

    private static final String BODY = "{\"ageOver14Agreed\":true,\"termsOfServiceAgreed\":true,"
        + "\"privacyPolicyAgreed\":true}";

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

    private String agreementBody(String omitted, String declined) {
        List<String> fields = new java.util.ArrayList<>();
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
        String accessToken = tokens.issue(1L).accessToken();
        clearInvocations(users);
        return post("/api/v1/auth/sign-up").contentType(MediaType.APPLICATION_JSON).content(body)
            .header("Authorization", "Bearer " + accessToken);
    }
}
