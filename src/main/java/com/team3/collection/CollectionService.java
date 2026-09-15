package com.team3.collection;

import java.util.List;

import com.team3.collection.dto.CollectionResponse;
import com.team3.collection.dto.CollectionsResponse;
import com.team3.collection.exception.DuplicateCollectionNameException;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CollectionService {

    private final CollectionRepository collections;

    public CollectionService(CollectionRepository collections) {
        this.collections = collections;
    }

    public CollectionResponse createCollection(Long userId, String name) {
        if (collections.existsByUserIdAndName(userId, name)) {
            throw nameConflict();
        }

        try {
            Collection created = collections.saveAndFlush(new Collection(userId, name));
            return new CollectionResponse(created.id(), created.name());
        } catch (DataIntegrityViolationException ex) {
            if (isNameConflict(ex)) {
                throw nameConflict();
            }
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public CollectionsResponse getCollections(Long userId, Sort sort) {
        List<CollectionResponse> responses = collections.findAllByUserId(userId, sort).stream()
            .map(collection -> new CollectionResponse(collection.id(), collection.name()))
            .toList();
        return new CollectionsResponse(responses);
    }

    private boolean isNameConflict(Throwable exception) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof ConstraintViolationException violation
                && "uk_collections_user_name".equals(violation.getConstraintName())) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    private DuplicateCollectionNameException nameConflict() {
        return new DuplicateCollectionNameException();
    }
}
