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
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Sort;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(WhiskyController.class)
@Import({SecurityConfig.class, WhiskyService.class})
class WhiskyHttpTests {

    private static final Limit SUGGESTION_LIMIT = Limit.of(10);
    private static final Sort SUGGESTION_SORT = Sort.by("id").ascending();

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private WhiskyRepository whiskies;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void returnsSuggestionsWithoutAuthentication() throws Exception {
        Whisky lagavulin = whisky(1L, "라가불린");
        Whisky lagavulin16 = whisky(2L, "라가불린 16");
        when(whiskies.findAllBy(SUGGESTION_SORT, SUGGESTION_LIMIT)).thenReturn(List.of(lagavulin, lagavulin16));

        mvc.perform(get("/api/v1/whiskies/suggestions"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").doesNotExist())
            .andExpect(jsonPath("$.data.suggestions.length()").value(2))
            .andExpect(jsonPath("$.data.suggestions[0].keyword").value("라가불린"))
            .andExpect(jsonPath("$.data.suggestions[1].keyword").value("라가불린 16"));
    }

    @Test
    void returnsEmptySuggestionsWhenNoneExist() throws Exception {
        when(whiskies.findAllBy(SUGGESTION_SORT, SUGGESTION_LIMIT)).thenReturn(List.of());

        mvc.perform(get("/api/v1/whiskies/suggestions"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.suggestions").isEmpty());
    }

    @Test
    void returnsMatchingSuggestionsForQuery() throws Exception {
        Whisky lagavulin16 = whisky(1L, "라가불린 16");
        when(whiskies.findByNameContaining("라가", SUGGESTION_SORT, SUGGESTION_LIMIT))
            .thenReturn(List.of(lagavulin16));

        mvc.perform(get("/api/v1/whiskies/suggestions").param("query", " 라가 "))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.suggestions.length()").value(1))
            .andExpect(jsonPath("$.data.suggestions[0].keyword").value("라가불린 16"));
    }

    @Test
    void rejectsBlankQuery() throws Exception {
        mvc.perform(get("/api/v1/whiskies/suggestions").param("query", "   "))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.detail").value("검색어가 올바르지 않습니다."));
    }

    @Test
    void rejectsOversizedQuery() throws Exception {
        mvc.perform(get("/api/v1/whiskies/suggestions").param("query", "a".repeat(256)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.detail").value("검색어가 올바르지 않습니다."));
    }

    private Whisky whisky(Long id, String name) {
        Whisky whisky = mock(Whisky.class);
        when(whisky.id()).thenReturn(id);
        when(whisky.name()).thenReturn(name);
        return whisky;
    }
}
