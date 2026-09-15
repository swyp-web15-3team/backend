package com.team3.collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;

import com.team3.security.SecurityConfig;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.hibernate.exception.ConstraintViolationException;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CollectionController.class)
@Import({SecurityConfig.class, CollectionService.class})
class CollectionHttpTests {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private CollectionService collectionService;

    @MockitoBean
    private CollectionRepository collections;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @BeforeEach
    void configureAccessToken() {
        Jwt jwt = Jwt.withTokenValue("access-token").header("alg", "HS256").subject("42")
            .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).build();
        when(jwtDecoder.decode("access-token")).thenReturn(jwt);
    }

    @Test
    void createsCollectionForAuthenticatedUserWithNormalizedName() throws Exception {
        Collection saved = mock(Collection.class);
        when(saved.id()).thenReturn(12L);
        when(saved.name()).thenReturn("선물 후보");
        when(collections.saveAndFlush(any(Collection.class))).thenReturn(saved);

        mvc.perform(post("/api/v1/collections").header("Authorization", "Bearer access-token")
            .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"  선물 후보  \"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.id").value(12))
            .andExpect(jsonPath("$.data.name").value("선물 후보"));

        ArgumentCaptor<Collection> captor = ArgumentCaptor.forClass(Collection.class);
        verify(collections).saveAndFlush(captor.capture());
        assertThat(captor.getValue().userId()).isEqualTo(42L);
        assertThat(captor.getValue().name()).isEqualTo("선물 후보");
    }

    @Test
    void requiresAuthentication() throws Exception {
        mvc.perform(post("/api/v1/collections").contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"선물 후보\"}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsInvalidName() throws Exception {
        mvc.perform(post("/api/v1/collections").header("Authorization", "Bearer access-token")
            .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"   \"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.errors[0].field").value("name"))
            .andExpect(jsonPath("$.errors[0].message").value("관심 그룹 이름은 필수입니다."));

        mvc.perform(post("/api/v1/collections").header("Authorization", "Bearer access-token")
            .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"" + "a".repeat(51) + "\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[0].message").value("관심 그룹 이름은 50자 이하여야 합니다."));
    }

    @Test
    void returnsConflictWhenNameAlreadyExists() throws Exception {
        when(collections.existsByUserIdAndName(42L, "선물 후보")).thenReturn(true);

        mvc.perform(post("/api/v1/collections").header("Authorization", "Bearer access-token")
            .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"선물 후보\"}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.code").value("COLLECTION_001"))
            .andExpect(jsonPath("$.detail").value("이미 존재하는 관심 그룹 이름입니다."));

        verify(collections, never()).saveAndFlush(any(Collection.class));
    }

    @Test
    void returnsConflictWhenConcurrentCreateViolatesUniqueConstraint() throws Exception {
        ConstraintViolationException violation = mock(ConstraintViolationException.class);
        when(violation.getConstraintName()).thenReturn("uk_collections_user_name");
        when(collections.saveAndFlush(any(Collection.class)))
            .thenThrow(new DataIntegrityViolationException("duplicate", violation));

        mvc.perform(post("/api/v1/collections").header("Authorization", "Bearer access-token")
            .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"선물 후보\"}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.code").value("COLLECTION_001"));
    }

    @Test
    void doesNotReportOtherIntegrityViolationsAsNameConflicts() {
        DataIntegrityViolationException violation = new DataIntegrityViolationException("foreign key");
        when(collections.saveAndFlush(any(Collection.class))).thenThrow(violation);

        assertThatThrownBy(() -> collectionService.createCollection(42L, "선물 후보")).isSameAs(violation);
    }
}
