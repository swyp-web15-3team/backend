package com.team3.whisky;

import java.util.List;

import com.team3.whisky.dto.WhiskySuggestionsResponse;
import com.team3.whisky.dto.WhiskySuggestionsResponse.Suggestion;

import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class WhiskyService {

    private static final int MAX_QUERY_LENGTH = 255;
    private static final Limit SUGGESTION_LIMIT = Limit.of(10);
    private static final Sort SUGGESTION_SORT = Sort.by("id").ascending();

    private final WhiskyRepository whiskies;

    public WhiskyService(WhiskyRepository whiskies) {
        this.whiskies = whiskies;
    }

    public WhiskySuggestionsResponse getSuggestions(String query) {
        List<Whisky> found;
        if (query == null) {
            found = whiskies.findAllBy(SUGGESTION_SORT, SUGGESTION_LIMIT);
        } else {
            String keyword = query.trim();
            if (keyword.isEmpty() || keyword.length() > MAX_QUERY_LENGTH) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "검색어가 올바르지 않습니다.");
            }
            found = whiskies.findByNameContaining(keyword, SUGGESTION_SORT, SUGGESTION_LIMIT);
        }
        List<Suggestion> suggestions = found.stream()
            .map(whisky -> new Suggestion(whisky.name()))
            .toList();
        return new WhiskySuggestionsResponse(suggestions);
    }
}
