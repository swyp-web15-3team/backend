package com.team3.collection;

import java.util.List;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CollectionWhiskyRepository extends JpaRepository<CollectionWhisky, Long> {
    boolean existsByCollectionIdAndWhiskyId(Long collectionId, Long whiskyId);

    List<CollectionWhisky> findAllByCollectionIdAndWhiskyIdIn(Long collectionId, Set<Long> whiskyIds);
}
