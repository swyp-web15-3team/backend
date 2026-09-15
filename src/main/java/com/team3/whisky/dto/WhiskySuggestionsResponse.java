package com.team3.whisky.dto;

import java.util.List;

public record WhiskySuggestionsResponse(List<Suggestion> suggestions) {

    public record Suggestion(String keyword) {
    }
}
