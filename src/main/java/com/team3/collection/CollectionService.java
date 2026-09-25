package com.team3.collection;

import java.util.List;
import java.util.LinkedHashSet;
import java.util.Set;

import com.team3.collection.dto.CollectionResponse;
import com.team3.collection.dto.CollectionsResponse;
import com.team3.collection.exception.CollectionNotFoundException;
import com.team3.collection.exception.DefaultCollectionImmutableException;
import com.team3.collection.exception.DuplicateCollectionNameException;
import com.team3.collection.exception.SameCollectionCopyException;
import com.team3.collection.exception.SameCollectionMoveException;
import com.team3.collection.exception.WhiskyNotFoundException;
import com.team3.whisky.WhiskyRepository;

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
    private final WhiskyRepository whiskies;

    public CollectionService(
        CollectionRepository collections,
        CollectionWhiskyRepository collectionWhiskies,
        WhiskyRepository whiskies) {
        this.collections = collections;
        this.collectionWhiskies = collectionWhiskies;
        this.whiskies = whiskies;
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
        assertWhiskiesExist(Set.of(whiskyId));
        if (!collectionWhiskies.existsByCollectionIdAndWhiskyId(collectionId, whiskyId)) {
            collectionWhiskies.save(new CollectionWhisky(collectionId, whiskyId));
        }
    }

    public void removeWhiskies(Long userId, Long collectionId, List<Long> whiskyIds) {
        findOwnedCollection(userId, collectionId);
        Set<Long> uniqueWhiskyIds = new LinkedHashSet<>(whiskyIds);
        assertWhiskiesExist(uniqueWhiskyIds);
        List<CollectionWhisky> memberships = collectionWhiskies
            .findAllByCollectionIdAndWhiskyIdIn(collectionId, uniqueWhiskyIds);
        collectionWhiskies.deleteAllInBatch(memberships);
    }

    public void moveWhiskies(Long userId, Long collectionId, Long targetCollectionId, List<Long> whiskyIds) {
        if (collectionId.equals(targetCollectionId)) {
            throw new SameCollectionMoveException();
        }
        findOwnedCollection(userId, collectionId);
        findOwnedCollection(userId, targetCollectionId);
        Set<Long> uniqueWhiskyIds = new LinkedHashSet<>(whiskyIds);
        assertWhiskiesExist(uniqueWhiskyIds);
        List<CollectionWhisky> memberships = collectionWhiskies
            .findAllByCollectionIdAndWhiskyIdIn(collectionId, uniqueWhiskyIds);
        collectionWhiskies.deleteAllInBatch(memberships);
        for (CollectionWhisky membership : memberships) {
            Long whiskyId = membership.whiskyId();
            if (!collectionWhiskies.existsByCollectionIdAndWhiskyId(targetCollectionId, whiskyId)) {
                collectionWhiskies.save(new CollectionWhisky(targetCollectionId, whiskyId));
            }
        }
    }

    public void copyWhiskies(Long userId, Long collectionId, Long targetCollectionId, List<Long> whiskyIds) {
        if (collectionId.equals(targetCollectionId)) {
            throw new SameCollectionCopyException();
        }
        findOwnedCollection(userId, collectionId);
        findOwnedCollection(userId, targetCollectionId);
        Set<Long> uniqueWhiskyIds = new LinkedHashSet<>(whiskyIds);
        assertWhiskiesExist(uniqueWhiskyIds);
        List<CollectionWhisky> memberships = collectionWhiskies
            .findAllByCollectionIdAndWhiskyIdIn(collectionId, uniqueWhiskyIds);
        for (CollectionWhisky membership : memberships) {
            Long whiskyId = membership.whiskyId();
            if (!collectionWhiskies.existsByCollectionIdAndWhiskyId(targetCollectionId, whiskyId)) {
                collectionWhiskies.save(new CollectionWhisky(targetCollectionId, whiskyId));
            }
        }
    }

    private void findOwnedCollection(Long userId, Long collectionId) {
        collections.findByIdAndUserId(collectionId, userId)
            .orElseThrow(CollectionNotFoundException::new);
    }

    private void assertWhiskiesExist(Set<Long> whiskyIds) {
        if (whiskies.countByIdIn(whiskyIds) != whiskyIds.size()) {
            throw new WhiskyNotFoundException();
        }
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
