package com.team3.collection;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CollectionRepository extends JpaRepository<Collection, Long> {
    boolean existsByUserIdAndName(Long userId, String name);

    List<Collection> findAllByUserId(Long userId, Sort sort);
}
