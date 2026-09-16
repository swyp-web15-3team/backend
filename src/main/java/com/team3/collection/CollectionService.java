package com.team3.collection;

import java.util.List;

import com.team3.collection.dto.CollectionResponse;
import com.team3.collection.dto.CollectionsResponse;
import com.team3.collection.exception.CollectionNotFoundException;
import com.team3.collection.exception.DefaultCollectionImmutableException;
import com.team3.collection.exception.DuplicateCollectionNameException;
import com.team3.collection.exception.WhiskyNotFoundException;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CollectionService {

    private final CollectionRepository collections;
    private final CollectionWhiskyRepository collectionWhiskies;

    public CollectionService(
        CollectionRepository collections,
        CollectionWhiskyRepository collectionWhiskies) {
        this.collections = collections;
        this.collectionWhiskies = collectionWhiskies;
    }

    public CollectionResponse createCollection(Long userId, String name) {
        if (collections.existsByUserIdAndName(userId, name)) {
            throw nameConflict();
        }

        try {
            Collection created = collections.saveAndFlush(new Collection(userId, name));
            return CollectionResponse.from(created);
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
            .map(CollectionResponse::from)
            .toList();
        return new CollectionsResponse(responses);
    }

    public CollectionResponse updateCollection(Long userId, Long collectionId, String name) {
        Collection collection = collections.findByIdAndUserId(collectionId, userId)
            .orElseThrow(CollectionNotFoundException::new);
        if (collection.isDefault()) {
            throw new DefaultCollectionImmutableException();
        }
        if (collections.existsByUserIdAndNameAndIdNot(userId, name, collectionId)) {
            throw nameConflict();
        }

        try {
            collection.updateName(name);
            collections.flush();
            return CollectionResponse.from(collection);
        } catch (DataIntegrityViolationException ex) {
            if (isNameConflict(ex)) {
                throw nameConflict();
            }
            throw ex;
        }
    }

    public void deleteCollection(Long userId, Long collectionId) {
        Collection collection = collections.findByIdAndUserId(collectionId, userId)
            .orElseThrow(CollectionNotFoundException::new);
        if (collection.isDefault()) {
            throw new DefaultCollectionImmutableException();
        }
        collections.delete(collection);
    }

    public void addWhisky(Long userId, Long collectionId, Long whiskyId) {
        findOwnedCollection(userId, collectionId);
        if (!collectionWhiskies.whiskyExists(whiskyId)) {
            throw new WhiskyNotFoundException();
        }
        collectionWhiskies.add(collectionId, whiskyId);
    }

    public void removeWhiskies(Long userId, Long collectionId, List<Long> whiskyIds) {
        findOwnedCollection(userId, collectionId);
        for (Long whiskyId : whiskyIds) {
            if (!collectionWhiskies.whiskyExists(whiskyId)) {
                throw new WhiskyNotFoundException();
            }
        }
        collectionWhiskies.removeAll(collectionId, whiskyIds);
    }

    private void findOwnedCollection(Long userId, Long collectionId) {
        collections.findByIdAndUserId(collectionId, userId)
            .orElseThrow(CollectionNotFoundException::new);
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
