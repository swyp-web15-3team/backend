package com.team3.collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.team3.security.SecurityConfig;
import com.team3.user.User;
import com.team3.user.enums.Provider;
import com.team3.user.UserRepository;
import com.team3.whisky.WhiskyRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.hibernate.exception.ConstraintViolationException;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
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
    private CollectionWhiskyRepository collectionWhiskies;

    @MockitoBean
    private WhiskyRepository whiskies;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private UserRepository users;

    @BeforeEach
    void configureAccessToken() {
        User user = new User(Provider.KAKAO, "42");
        user.activate();
        when(users.findById(42L)).thenReturn(Optional.of(user));
        Jwt jwt = Jwt.withTokenValue("access-token").header("alg", "HS256").subject("42")
            .issuedAt(Instant.now()).expiresAt(Instant.now().plusSeconds(60)).build();
        when(jwtDecoder.decode("access-token")).thenReturn(jwt);
    }

    @Test
    void pendingUserCannotReadOrModifyCollections() throws Exception {
        when(users.findById(42L)).thenReturn(Optional.of(new User(Provider.KAKAO, "42")));
        for (String method : List.of("GET", "POST", "PATCH", "DELETE")) {
            String path = method.equals("PATCH") || method.equals("DELETE")
                ? "/api/v1/collections/12"
                : "/api/v1/collections";
            mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .request(org.springframework.http.HttpMethod.valueOf(method), path)
                .header("Authorization", "Bearer access-token")
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"test\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("AUTH_005"));
        }
        org.mockito.Mockito.verifyNoInteractions(collections, collectionWhiskies, whiskies);
    }

    @Test
    void malformedUserIdIsDeniedWithoutQueryingRepositories() throws Exception {
        for (String subject : List.of("invalid", "0", "-1", "9223372036854775808")) {
            when(jwtDecoder.decode("access-token")).thenReturn(Jwt.withTokenValue("access-token")
                .header("alg", "HS256").subject(subject).build());
            mvc.perform(get("/api/v1/collections").header("Authorization", "Bearer access-token"))
                .andExpect(status().isForbidden());
        }
        org.mockito.Mockito.verifyNoInteractions(users, collections, collectionWhiskies, whiskies);
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
            .andExpect(jsonPath("$.data.name").value("선물 후보"))
            .andExpect(jsonPath("$.data.isDefault").value(false));

        ArgumentCaptor<Collection> captor = ArgumentCaptor.forClass(Collection.class);
        verify(collections).saveAndFlush(captor.capture());
        assertThat(captor.getValue().userId()).isEqualTo(42L);
        assertThat(captor.getValue().name()).isEqualTo("선물 후보");
    }

    @Test
    void returnsAuthenticatedUsersCollectionsInRepositoryOrder() throws Exception {
        Collection latest = collection(13L, "이번 주말");
        Collection older = collection(12L, "선물 후보");
        Sort defaultSort = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
        when(collections.findAllByUserId(42L, defaultSort)).thenReturn(List.of(latest, older));

        mvc.perform(get("/api/v1/collections").header("Authorization", "Bearer access-token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.collections.length()").value(2))
            .andExpect(jsonPath("$.data.collections[0].id").value(13))
            .andExpect(jsonPath("$.data.collections[0].name").value("이번 주말"))
            .andExpect(jsonPath("$.data.collections[0].isDefault").value(false))
            .andExpect(jsonPath("$.data.collections[1].id").value(12))
            .andExpect(jsonPath("$.data.collections[1].name").value("선물 후보"));

        verify(collections).findAllByUserId(42L, defaultSort);
    }

    @Test
    void usesRequestedCollectionSort() throws Exception {
        Sort requestedSort = Sort.by(Sort.Order.asc("name"));
        when(collections.findAllByUserId(42L, requestedSort)).thenReturn(List.of());

        mvc.perform(get("/api/v1/collections").header("Authorization", "Bearer access-token")
            .param("sort", "name,asc"))
            .andExpect(status().isOk());

        verify(collections).findAllByUserId(42L, requestedSort);
    }

    @Test
    void returnsEmptyCollectionsWhenNoneExist() throws Exception {
        mvc.perform(get("/api/v1/collections").header("Authorization", "Bearer access-token"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.collections").isEmpty());
    }

    @Test
    void updatesOwnedCollectionWithNormalizedName() throws Exception {
        Collection collection = collection(12L, "새 이름");
        when(collections.findByIdAndUserId(12L, 42L)).thenReturn(Optional.of(collection));

        mvc.perform(patch("/api/v1/collections/12").header("Authorization", "Bearer access-token")
            .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"  새 이름  \"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").value(12))
            .andExpect(jsonPath("$.data.name").value("새 이름"))
            .andExpect(jsonPath("$.data.isDefault").value(false));

        verify(collection).updateName("새 이름");
        verify(collections).flush();
    }

    @Test
    void normalizesNameWhenUpdatingCollectionEntity() {
        Collection collection = new Collection(42L, "기존 이름");

        collection.updateName("  새 이름  ");

        assertThat(collection.name()).isEqualTo("새 이름");
    }

    @Test
    void returnsNotFoundWhenCollectionIsMissingOrNotOwned() throws Exception {
        when(collections.findByIdAndUserId(12L, 42L)).thenReturn(Optional.empty());

        mvc.perform(patch("/api/v1/collections/12").header("Authorization", "Bearer access-token")
            .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"새 이름\"}"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("COLLECTION_002"));

        verify(collections, never()).flush();
    }

    @Test
    void rejectsUpdatingDefaultCollection() throws Exception {
        Collection collection = collection(12L, "기본 관심 목록", true);
        when(collections.findByIdAndUserId(12L, 42L)).thenReturn(Optional.of(collection));

        mvc.perform(patch("/api/v1/collections/12").header("Authorization", "Bearer access-token")
            .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"새 이름\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("COLLECTION_003"));

        verify(collections, never()).flush();
    }

    @Test
    void deletesOwnedNonDefaultCollection() throws Exception {
        Collection collection = collection(12L, "선물 후보");
        when(collections.findByIdAndUserId(12L, 42L)).thenReturn(Optional.of(collection));

        mvc.perform(delete("/api/v1/collections/12").header("Authorization", "Bearer access-token"))
            .andExpect(status().isNoContent())
            .andExpect(content().string(""));

        verify(collections).delete(collection);
    }

    @Test
    void returnsNotFoundWhenDeletingCollectionIsMissingOrNotOwned() throws Exception {
        when(collections.findByIdAndUserId(12L, 42L)).thenReturn(Optional.empty());

        mvc.perform(delete("/api/v1/collections/12").header("Authorization", "Bearer access-token"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("COLLECTION_002"));

        verify(collections, never()).delete(any(Collection.class));
    }

    @Test
    void rejectsDeletingDefaultCollection() throws Exception {
        Collection collection = collection(12L, "기본 관심 목록", true);
        when(collections.findByIdAndUserId(12L, 42L)).thenReturn(Optional.of(collection));

        mvc.perform(delete("/api/v1/collections/12").header("Authorization", "Bearer access-token"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("COLLECTION_003"));

        verify(collections, never()).delete(any(Collection.class));
    }

    @Test
    void requiresAuthenticationToDeleteCollection() throws Exception {
        mvc.perform(delete("/api/v1/collections/12"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void returnsConflictWhenUpdatedNameAlreadyExists() throws Exception {
        Collection collection = collection(12L, "새 이름");
        when(collections.findByIdAndUserId(12L, 42L)).thenReturn(Optional.of(collection));
        when(collections.existsByUserIdAndNameAndIdNot(42L, "새 이름", 12L)).thenReturn(true);

        mvc.perform(patch("/api/v1/collections/12").header("Authorization", "Bearer access-token")
            .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"새 이름\"}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("COLLECTION_001"));

        verify(collections, never()).flush();
    }

    @Test
    void returnsConflictWhenConcurrentUpdateViolatesUniqueConstraint() throws Exception {
        Collection collection = collection(12L, "새 이름");
        ConstraintViolationException violation = mock(ConstraintViolationException.class);
        when(violation.getConstraintName()).thenReturn("uk_collections_user_name");
        when(collections.findByIdAndUserId(12L, 42L)).thenReturn(Optional.of(collection));
        doThrow(new DataIntegrityViolationException("duplicate", violation))
            .when(collections).flush();

        mvc.perform(patch("/api/v1/collections/12").header("Authorization", "Bearer access-token")
            .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"새 이름\"}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("COLLECTION_001"));
    }

    @Test
    void requiresAuthentication() throws Exception {
        mvc.perform(post("/api/v1/collections").contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"선물 후보\"}"))
            .andExpect(status().isUnauthorized());

        mvc.perform(get("/api/v1/collections"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("AUTH_001"));

        mvc.perform(patch("/api/v1/collections/12").contentType(MediaType.APPLICATION_JSON)
            .content("{\"name\":\"새 이름\"}"))
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

        mvc.perform(patch("/api/v1/collections/12").header("Authorization", "Bearer access-token")
            .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"   \"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[0].field").value("name"));
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

    @Test
    void addsWhiskyToOwnedDefaultCollectionIdempotently() throws Exception {
        Collection collection = collection(12L, "기본 관심 목록", true);
        when(collections.findByIdAndUserId(12L, 42L)).thenReturn(Optional.of(collection));
        when(whiskies.countByIdIn(Set.of(101L))).thenReturn(1L);
        when(collectionWhiskies.existsByCollectionIdAndWhiskyId(12L, 101L)).thenReturn(false, true);

        for (int index = 0; index < 2; index++) {
            mvc.perform(post("/api/v1/collections/12/whiskies").header("Authorization", "Bearer access-token")
                .contentType(MediaType.APPLICATION_JSON).content("{\"whiskyId\":101}"))
                .andExpect(status().isNoContent()).andExpect(content().string(""));
        }

        verify(collectionWhiskies, times(2)).existsByCollectionIdAndWhiskyId(12L, 101L);
        verify(collectionWhiskies).save(any(CollectionWhisky.class));
    }

    @Test
    void removesKnownWhiskiesIncludingNonmembers() throws Exception {
        Collection collection = collection(12L, "선물 후보");
        CollectionWhisky membership = mock(CollectionWhisky.class);
        when(collections.findByIdAndUserId(12L, 42L)).thenReturn(Optional.of(collection));
        when(whiskies.countByIdIn(Set.of(101L, 102L))).thenReturn(2L);
        when(collectionWhiskies.findAllByCollectionIdAndWhiskyIdIn(12L, Set.of(101L, 102L)))
            .thenReturn(List.of(membership));

        mvc.perform(delete("/api/v1/collections/12/whiskies").header("Authorization", "Bearer access-token")
            .param("whiskyIds", "101", "102", "101"))
            .andExpect(status().isNoContent()).andExpect(content().string(""));

        verify(collectionWhiskies).findAllByCollectionIdAndWhiskyIdIn(12L, Set.of(101L, 102L));
        verify(collectionWhiskies).deleteAllInBatch(List.of(membership));
    }

    @Test
    void rejectsUnauthenticatedAndUnownedWhiskyChanges() throws Exception {
        mvc.perform(post("/api/v1/collections/12/whiskies").contentType(MediaType.APPLICATION_JSON)
            .content("{\"whiskyId\":101}")).andExpect(status().isUnauthorized());
        when(collections.findByIdAndUserId(12L, 42L)).thenReturn(Optional.empty());

        mvc.perform(delete("/api/v1/collections/12/whiskies").header("Authorization", "Bearer access-token")
            .param("whiskyIds", "101")).andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("COLLECTION_002"));

        verify(whiskies, never()).countByIdIn(any());
    }

    @Test
    void rejectsUnknownWhiskyWithoutRemovingAnyMemberships() throws Exception {
        Collection collection = collection(12L, "선물 후보");
        when(collections.findByIdAndUserId(12L, 42L)).thenReturn(Optional.of(collection));

        mvc.perform(post("/api/v1/collections/12/whiskies").header("Authorization", "Bearer access-token")
            .contentType(MediaType.APPLICATION_JSON).content("{\"whiskyId\":102}")).andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("WHISKY_001"));
        mvc.perform(delete("/api/v1/collections/12/whiskies").header("Authorization", "Bearer access-token")
            .param("whiskyIds", "101", "102")).andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("WHISKY_001"));

        verify(collectionWhiskies, never()).deleteAllInBatch(any());
    }

    @Test
    void rejectsInvalidWhiskyMembershipInput() throws Exception {
        mvc.perform(post("/api/v1/collections/12/whiskies").header("Authorization", "Bearer access-token")
            .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors[0].field").value("whiskyId"));
        mvc.perform(post("/api/v1/collections/12/whiskies").header("Authorization", "Bearer access-token")
            .contentType(MediaType.APPLICATION_JSON).content("{\"whiskyId\":0}")).andExpect(status().isBadRequest());
        mvc.perform(delete("/api/v1/collections/12/whiskies").header("Authorization", "Bearer access-token"))
            .andExpect(status().isBadRequest());
        mvc.perform(delete("/api/v1/collections/12/whiskies").header("Authorization", "Bearer access-token")
            .param("whiskyIds", "0")).andExpect(status().isBadRequest());
        mvc.perform(delete("/api/v1/collections/12/whiskies").header("Authorization", "Bearer access-token")
            .param("whiskyIds", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16",
                "17", "18", "19", "20", "21"))
            .andExpect(status().isBadRequest());
    }

    private Collection collection(Long id, String name) {
        return collection(id, name, false);
    }

    private Collection collection(Long id, String name, boolean isDefault) {
        Collection collection = mock(Collection.class);
        when(collection.id()).thenReturn(id);
        when(collection.name()).thenReturn(name);
        when(collection.isDefault()).thenReturn(isDefault);
        return collection;
    }
}
