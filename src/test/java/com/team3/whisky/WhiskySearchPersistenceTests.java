package com.team3.whisky;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.TestPropertySource;

@Tag("integration")
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@EnabledIfEnvironmentVariable(named = "TEST_DATABASE_URL", matches = "jdbc:postgresql://[^/]+/[^?]*(?i:test|integration)[^?]*")
@TestPropertySource(properties = {
        "spring.datasource.url=${TEST_DATABASE_URL}",
        "spring.datasource.username=${TEST_DATABASE_USERNAME}",
        "spring.datasource.password=${TEST_DATABASE_PASSWORD}"
})
class WhiskySearchPersistenceTests {

    @Autowired
    private WhiskyRepository whiskies;
    @Autowired
    private EntityManager entityManager;
    @Test
    void filtersLatestConvertedPricesAndDiscountsBeforeCountingAndPaging() {
        execute("insert into whisky_categories(id,name) values (990001,'one'),(990002,'two')");
        execute(
            """
                insert into whiskies(id,category_id,name,volume_ml) values (990011,990001,'search-a',700),(990012,990002,'search-b',700),
                  (990013,990001,'search-c',700),(990014,990002,'search-d',700)
                """);
        execute(
            """
                insert into retailers(id,name,country_code,is_duty_free) values (990021,'kr','KR',false),(990022,'jp','JP',false)
                """);
        execute(
            """
                insert into sale_products(id,whisky_id,retailer_id,external_product_id,raw_name,product_url,is_sold_out) values
                  (990031,990011,990021,'a-kr','a','url',false),(990032,990011,990022,'a-jp','a','url',false),(990033,990012,990021,'b-kr','b','url',false),
                  (990034,990013,990021,'c-kr','c','url',false),(990035,990014,990021,'d-kr','d','url',false),(990036,990014,990022,'d-jp','d','url',false),
                  (990037,990013,990021,'c-sold','c','url',true)
                """);
        execute(
            """
                insert into price_histories(id,sale_product_id,price,currency_code,collected_at) values
                  (990041,990031,80000,'KRW','2026-01-01T00:00:00Z'),(990042,990031,120000,'KRW','2026-02-01T00:00:00Z'),(990043,990032,1000,'JPY','2026-02-01T00:00:00Z'),
                  (990044,990033,150000,'KRW','2026-02-01T00:00:00Z'),(990045,990034,110000,'KRW','2026-02-01T00:00:00Z'),(990046,990035,100000,'KRW','2026-02-01T00:00:00Z'),
                  (990047,990036,850,'JPY','2026-02-01T00:00:00Z'),(990048,990037,1000,'KRW','2026-02-01T00:00:00Z')
                """);
        PageRequest first = PageRequest.of(0, 1, Sort.by("id").ascending());
        Page<Whisky> page = search(new BigDecimal("100000"), new BigDecimal("110000"), null, null, first);
        assertThat(page.getContent()).extracting(Whisky::name).containsExactly("search-a");
        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(search(new BigDecimal("100000"), new BigDecimal("110000"), null, null, first.next())
            .getContent()).extracting(Whisky::name).containsExactly("search-c");

        Page<Whisky> discount = search(null, null, new BigDecimal("15"), new BigDecimal("17"),
            PageRequest.of(0, 10, Sort.by("id").ascending()));
        assertThat(discount.getContent()).extracting(Whisky::name).containsExactly("search-a", "search-d");
        assertThat(search(null, null, null, new BigDecimal("15"), first).getTotalElements()).isZero();
    }

    private Page<Whisky> search(BigDecimal min, BigDecimal max, BigDecimal minDiscount, BigDecimal maxDiscount,
        PageRequest page) {
        return whiskies.search(null, List.of(990001L, 990002L), null, null, null, null, null, min, max, minDiscount,
            maxDiscount, new BigDecimal("100"), page);
    }

    private void execute(String sql) {
        entityManager.createNativeQuery(sql).executeUpdate();
    }
}
