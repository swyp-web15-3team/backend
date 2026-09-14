package com.team3.collection;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CollectionWhiskyRepository {

    private final JdbcTemplate jdbcTemplate;

    public CollectionWhiskyRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public boolean whiskyExists(Long whiskyId) {
        Boolean exists = jdbcTemplate.queryForObject(
            "SELECT EXISTS (SELECT 1 FROM whiskies WHERE id = ?)", Boolean.class, whiskyId);
        return Boolean.TRUE.equals(exists);
    }

    public void add(Long collectionId, Long whiskyId) {
        jdbcTemplate.update(
            "INSERT INTO collection_whiskies (collection_id, whisky_id) VALUES (?, ?) ON CONFLICT DO NOTHING",
            collectionId, whiskyId);
    }

    public void removeAll(Long collectionId, List<Long> whiskyIds) {
        String placeholders = whiskyIds.stream().map(whiskyId -> "?").collect(Collectors.joining(", "));
        List<Object> parameters = new ArrayList<>();
        parameters.add(collectionId);
        parameters.addAll(whiskyIds);
        jdbcTemplate.update(
            "DELETE FROM collection_whiskies WHERE collection_id = ? AND whisky_id IN (" + placeholders + ")",
            parameters.toArray());
    }
}
