package com.team3.whisky;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import com.team3.security.SecurityConfig;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(WhiskyCategoryController.class)
@Import({SecurityConfig.class, WhiskyCategoryService.class})
class WhiskyCategoryHttpTests {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private WhiskyCategoryRepository categories;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private com.team3.user.UserRepository users;

    @Test
    void returnsCategoriesWithoutAuthentication() throws Exception {
        WhiskyCategory singleMalt = category(1L, "싱글 몰트");
        WhiskyCategory blended = category(2L, "블렌디드");
        when(categories.findAllByOrderByIdAsc()).thenReturn(List.of(singleMalt, blended));

        mvc.perform(get("/api/v1/whisky-categories"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").doesNotExist())
            .andExpect(jsonPath("$.data.categories.length()").value(2))
            .andExpect(jsonPath("$.data.categories[0].id").value(1))
            .andExpect(jsonPath("$.data.categories[0].name").value("싱글 몰트"))
            .andExpect(jsonPath("$.data.categories[1].id").value(2))
            .andExpect(jsonPath("$.data.categories[1].name").value("블렌디드"));
    }

    @Test
    void returnsEmptyCategoriesWhenNoneExist() throws Exception {
        when(categories.findAllByOrderByIdAsc()).thenReturn(List.of());

        mvc.perform(get("/api/v1/whisky-categories"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.categories").isEmpty());
    }

    private WhiskyCategory category(Long id, String name) {
        WhiskyCategory category = mock(WhiskyCategory.class);
        when(category.id()).thenReturn(id);
        when(category.name()).thenReturn(name);
        return category;
    }
}
