package com.team3.whisky;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SaleProductRepository extends JpaRepository<SaleProduct, Long> {

    @EntityGraph(attributePaths = "retailer")
    List<SaleProduct> findByWhiskyIdOrderByIdAsc(Long whiskyId);
}
