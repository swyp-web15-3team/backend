package com.team3.collection;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CollectionRepository extends JpaRepository<Collection, Long> {
    boolean existsByUserIdAndName(Long userId, String name);

    boolean existsByUserIdAndNameAndIdNot(Long userId, String name, Long id);

    Optional<Collection> findByIdAndUserId(Long id, Long userId);

    List<Collection> findAllByUserId(Long userId, Sort sort);
}
