package com.team3.whisky;

import java.util.List;

import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WhiskyRepository extends JpaRepository<Whisky, Long> {

    List<Whisky> findAllBy(Sort sort, Limit limit);

    List<Whisky> findByNameContaining(String name, Sort sort, Limit limit);
}
