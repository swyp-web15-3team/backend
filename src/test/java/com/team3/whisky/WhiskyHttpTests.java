package com.team3.whisky;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.team3.security.SecurityConfig;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(WhiskyController.class)
@Import({SecurityConfig.class, WhiskyService.class})
class WhiskyHttpTests {

    private static final Limit SUGGESTION_LIMIT = Limit.of(10);
    private static final Sort SUGGESTION_SORT = Sort.by("id").ascending();
    private static final Instant COLLECTED_AT = Instant.parse("2026-09-07T18:00:00Z");

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private WhiskyRepository whiskies;

    @MockitoBean
    private WhiskyCategoryRepository categories;

    @MockitoBean
    private WhiskyOriginRepository origins;

    @MockitoBean
    private WhiskyRegionRepository regions;

    @MockitoBean
    private PriceHistoryRepository prices;

    @MockitoBean
    private SaleProductRepository saleProducts;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private com.team3.user.UserRepository users;

    @Test
    void returnsSuggestionsWithoutAuthentication() throws Exception {
        Whisky lagavulin = whisky(1L, "라가불린");
        Whisky lagavulin16 = whisky(2L, "라가불린 16");
        when(whiskies.findAllBy(SUGGESTION_SORT, SUGGESTION_LIMIT)).thenReturn(List.of(lagavulin, lagavulin16));

        mvc.perform(get("/api/v1/whiskies/suggestions"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").doesNotExist())
            .andExpect(jsonPath("$.data.suggestions.length()").value(2))
            .andExpect(jsonPath("$.data.suggestions[0].keyword").value("라가불린"))
            .andExpect(jsonPath("$.data.suggestions[1].keyword").value("라가불린 16"));
    }

    @Test
    void returnsEmptySuggestionsWhenNoneExist() throws Exception {
        when(whiskies.findAllBy(SUGGESTION_SORT, SUGGESTION_LIMIT)).thenReturn(List.of());

        mvc.perform(get("/api/v1/whiskies/suggestions"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.suggestions").isEmpty());
    }

    @Test
    void returnsMatchingSuggestionsForQuery() throws Exception {
        Whisky lagavulin16 = whisky(1L, "라가불린 16");
        when(whiskies.findByNameContaining("라가", SUGGESTION_SORT, SUGGESTION_LIMIT))
            .thenReturn(List.of(lagavulin16));

        mvc.perform(get("/api/v1/whiskies/suggestions").param("query", " 라가 "))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.suggestions.length()").value(1))
            .andExpect(jsonPath("$.data.suggestions[0].keyword").value("라가불린 16"));
    }

    @Test
    void rejectsBlankQuery() throws Exception {
        mvc.perform(get("/api/v1/whiskies/suggestions").param("query", "   "))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.detail").value("검색어가 올바르지 않습니다."));
    }

    @Test
    void rejectsOversizedQuery() throws Exception {
        mvc.perform(get("/api/v1/whiskies/suggestions").param("query", "a".repeat(256)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.detail").value("검색어가 올바르지 않습니다."));
    }

    @Test
    void returnsWhiskiesWithoutAuthentication() throws Exception {
        Whisky lagavulin16 = listedWhisky();
        when(whiskies.search(isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(lagavulin16), PageRequest.of(0, 20), 1));
        when(prices.findLatestAvailablePrices(List.of(101L))).thenReturn(List.of(
            new WhiskyLatestPrice(
                101L, new BigDecimal("189000"), "KRW", "KR", "롯데면세점", COLLECTED_AT, 1L),
            new WhiskyLatestPrice(
                101L, new BigDecimal("9800"), "JPY", "JP", "나리타 면세", COLLECTED_AT, 2L)));

        mvc.perform(get("/api/v1/whiskies"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").doesNotExist())
            .andExpect(jsonPath("$.data.content.length()").value(1))
            .andExpect(jsonPath("$.data.content[0].id").value(101))
            .andExpect(jsonPath("$.data.content[0].name").value("Lagavulin 16"))
            .andExpect(jsonPath("$.data.content[0].volumeMl").value(700))
            .andExpect(jsonPath("$.data.content[0].abv").value(43.0))
            .andExpect(jsonPath("$.data.content[0].category.id").value(1))
            .andExpect(jsonPath("$.data.content[0].category.name").value("싱글 몰트"))
            .andExpect(jsonPath("$.data.content[0].origin.id").value(1))
            .andExpect(jsonPath("$.data.content[0].origin.name").value("스코틀랜드"))
            .andExpect(jsonPath("$.data.content[0].region.id").value(10))
            .andExpect(jsonPath("$.data.content[0].region.name").value("아일라"))
            .andExpect(jsonPath("$.data.content[0].kr.amount").value(189000))
            .andExpect(jsonPath("$.data.content[0].kr.currency").value("KRW"))
            .andExpect(jsonPath("$.data.content[0].kr.retailerName").value("롯데면세점"))
            .andExpect(jsonPath("$.data.content[0].kr.collectedAt").value("2026-09-07T18:00:00Z"))
            .andExpect(jsonPath("$.data.content[0].kr.stale").value(false))
            .andExpect(jsonPath("$.data.content[0].jp.amount").value(9800))
            .andExpect(jsonPath("$.data.content[0].jp.currency").value("JPY"))
            .andExpect(jsonPath("$.data.content[0].jp.amountKrw").isEmpty())
            .andExpect(jsonPath("$.data.content[0].jp.retailerName").value("나리타 면세"))
            .andExpect(jsonPath("$.data.content[0].jp.stale").value(false))
            .andExpect(jsonPath("$.data.content[0].comparison").isEmpty())
            .andExpect(jsonPath("$.data.page").value(0))
            .andExpect(jsonPath("$.data.size").value(20))
            .andExpect(jsonPath("$.data.totalElements").value(1))
            .andExpect(jsonPath("$.data.totalPages").value(1));
    }

    @Test
    void returnsEmptyWhiskiesWhenNoneExist() throws Exception {
        when(whiskies.search(isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mvc.perform(get("/api/v1/whiskies"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.content").isEmpty())
            .andExpect(jsonPath("$.data.totalElements").value(0))
            .andExpect(jsonPath("$.data.totalPages").value(0));
    }

    @Test
    void returnsMatchingWhiskiesForQuery() throws Exception {
        Whisky lagavulin16 = listedWhisky();
        when(whiskies.search(eq("라가"), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
            .thenReturn(new PageImpl<>(List.of(lagavulin16), PageRequest.of(0, 20), 1));
        when(prices.findLatestAvailablePrices(List.of(101L))).thenReturn(List.of());

        mvc.perform(get("/api/v1/whiskies").param("query", " 라가 "))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.content.length()").value(1))
            .andExpect(jsonPath("$.data.content[0].kr").isEmpty())
            .andExpect(jsonPath("$.data.content[0].jp").isEmpty());
    }

    @Test
    void rejectsBlankListQuery() throws Exception {
        mvc.perform(get("/api/v1/whiskies").param("query", "   "))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("검색어가 올바르지 않습니다."));
    }

    @Test
    void rejectsInvalidPageSize() throws Exception {
        mvc.perform(get("/api/v1/whiskies").param("size", "51"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("size는 1 이상 50 이하여야 합니다."));
    }

    @Test
    void rejectsUnsupportedSort() throws Exception {
        mvc.perform(get("/api/v1/whiskies").param("sort", "price,asc"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("지원하지 않는 정렬 조건입니다."));
    }

    @Test
    void rejectsInvalidCountryCode() throws Exception {
        mvc.perform(get("/api/v1/whiskies").param("countryCode", "US"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("판매 국가는 KR 또는 JP여야 합니다."));
    }

    @Test
    void rejectsUnknownFilterId() throws Exception {
        when(categories.existsById(99L)).thenReturn(false);

        mvc.perform(get("/api/v1/whiskies").param("categoryId", "99"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("존재하지 않는 필터 값입니다."));
    }

    @Test
    void rejectsMismatchedRegionAndOrigin() throws Exception {
        when(origins.existsById(1L)).thenReturn(true);
        WhiskyOrigin scotland = mock(WhiskyOrigin.class);
        when(scotland.id()).thenReturn(2L);
        WhiskyRegion islay = mock(WhiskyRegion.class);
        when(islay.origin()).thenReturn(scotland);
        when(regions.findById(10L)).thenReturn(Optional.of(islay));

        mvc.perform(get("/api/v1/whiskies").param("originId", "1").param("regionId", "10"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detail").value("생산 지역과 원산지가 일치하지 않습니다."));
    }

    @Test
    void returnsWhiskyDetailWithoutAuthentication() throws Exception {
        Whisky whisky = listedWhisky();
        when(whiskies.findById(101L)).thenReturn(Optional.of(whisky));
        SaleProduct krProduct = saleProduct(
            501L, "롯데면세점", "서울특별시 중구 을지로 30", "KR", true, "https://example.com/product/501", false);
        SaleProduct soldOutProduct = saleProduct(
            503L, "품절점", null, "KR", false, "https://example.com/product/503", true);
        when(saleProducts.findByWhiskyIdOrderByIdAsc(101L)).thenReturn(List.of(krProduct, soldOutProduct));
        when(prices.findLatestAvailablePrices(List.of(101L))).thenReturn(List.of(
            new WhiskyLatestPrice(
                101L, new BigDecimal("189000"), "KRW", "KR", "롯데면세점", COLLECTED_AT, 501L),
            new WhiskyLatestPrice(
                101L, new BigDecimal("9800"), "JPY", "JP", "나리타 면세", COLLECTED_AT, 502L)));
        when(prices.findLatestPrices(List.of(501L, 503L))).thenReturn(List.of(
            new WhiskyLatestPrice(
                101L, new BigDecimal("189000"), "KRW", "KR", "롯데면세점", COLLECTED_AT, 501L),
            new WhiskyLatestPrice(
                101L, new BigDecimal("200000"), "KRW", "KR", "품절점", COLLECTED_AT, 503L)));

        mvc.perform(get("/api/v1/whiskies/101"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").doesNotExist())
            .andExpect(jsonPath("$.data.id").value(101))
            .andExpect(jsonPath("$.data.name").value("Lagavulin 16"))
            .andExpect(jsonPath("$.data.volumeMl").value(700))
            .andExpect(jsonPath("$.data.abv").value(43.0))
            .andExpect(jsonPath("$.data.category.id").value(1))
            .andExpect(jsonPath("$.data.category.name").value("싱글 몰트"))
            .andExpect(jsonPath("$.data.origin.id").value(1))
            .andExpect(jsonPath("$.data.origin.name").value("스코틀랜드"))
            .andExpect(jsonPath("$.data.region.id").value(10))
            .andExpect(jsonPath("$.data.region.name").value("아일라"))
            .andExpect(jsonPath("$.data.kr.amount").value(189000))
            .andExpect(jsonPath("$.data.kr.currency").value("KRW"))
            .andExpect(jsonPath("$.data.kr.retailerName").value("롯데면세점"))
            .andExpect(jsonPath("$.data.kr.collectedAt").value("2026-09-07T18:00:00Z"))
            .andExpect(jsonPath("$.data.kr.stale").value(false))
            .andExpect(jsonPath("$.data.jp.amount").value(9800))
            .andExpect(jsonPath("$.data.jp.currency").value("JPY"))
            .andExpect(jsonPath("$.data.jp.amountKrw").isEmpty())
            .andExpect(jsonPath("$.data.jp.retailerName").value("나리타 면세"))
            .andExpect(jsonPath("$.data.jp.stale").value(false))
            .andExpect(jsonPath("$.data.comparison").isEmpty())
            .andExpect(jsonPath("$.data.saleProducts.length()").value(2))
            .andExpect(jsonPath("$.data.saleProducts[0].id").value(501))
            .andExpect(jsonPath("$.data.saleProducts[0].retailerName").value("롯데면세점"))
            .andExpect(jsonPath("$.data.saleProducts[0].retailerAddress").value("서울특별시 중구 을지로 30"))
            .andExpect(jsonPath("$.data.saleProducts[0].countryCode").value("KR"))
            .andExpect(jsonPath("$.data.saleProducts[0].isDutyFree").value(true))
            .andExpect(jsonPath("$.data.saleProducts[0].productUrl").value("https://example.com/product/501"))
            .andExpect(jsonPath("$.data.saleProducts[0].isSoldOut").value(false))
            .andExpect(jsonPath("$.data.saleProducts[0].price.amount").value(189000))
            .andExpect(jsonPath("$.data.saleProducts[0].price.currency").value("KRW"))
            .andExpect(jsonPath("$.data.saleProducts[0].price.amountKrw").isEmpty())
            .andExpect(jsonPath("$.data.saleProducts[0].price.collectedAt").value("2026-09-07T18:00:00Z"))
            .andExpect(jsonPath("$.data.saleProducts[0].price.stale").value(false))
            .andExpect(jsonPath("$.data.saleProducts[1].id").value(503))
            .andExpect(jsonPath("$.data.saleProducts[1].isSoldOut").value(true))
            .andExpect(jsonPath("$.data.saleProducts[1].retailerAddress").isEmpty())
            .andExpect(jsonPath("$.data.saleProducts[1].price.amount").value(200000));
    }

    @Test
    void returnsEmptySaleProductsWhenNoneExist() throws Exception {
        Whisky whisky = listedWhisky();
        when(whiskies.findById(101L)).thenReturn(Optional.of(whisky));
        when(saleProducts.findByWhiskyIdOrderByIdAsc(101L)).thenReturn(List.of());
        when(prices.findLatestAvailablePrices(List.of(101L))).thenReturn(List.of());

        mvc.perform(get("/api/v1/whiskies/101"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.saleProducts").isEmpty())
            .andExpect(jsonPath("$.data.kr").isEmpty())
            .andExpect(jsonPath("$.data.jp").isEmpty())
            .andExpect(jsonPath("$.data.comparison").isEmpty());
    }

    @Test
    void returnsNullPriceWhenSaleProductHasNoHistory() throws Exception {
        Whisky whisky = listedWhisky();
        when(whiskies.findById(101L)).thenReturn(Optional.of(whisky));
        SaleProduct noPrice = saleProduct(
            501L, "롯데면세점", "서울특별시 중구 을지로 30", "KR", true, "https://example.com/product/501", null);
        when(saleProducts.findByWhiskyIdOrderByIdAsc(101L)).thenReturn(List.of(noPrice));
        when(prices.findLatestAvailablePrices(List.of(101L))).thenReturn(List.of());
        when(prices.findLatestPrices(List.of(501L))).thenReturn(List.of());

        mvc.perform(get("/api/v1/whiskies/101"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.saleProducts[0].isSoldOut").isEmpty())
            .andExpect(jsonPath("$.data.saleProducts[0].price").isEmpty())
            .andExpect(jsonPath("$.data.kr").isEmpty());
    }

    @Test
    void rejectsUnknownWhisky() throws Exception {
        when(whiskies.findById(101L)).thenReturn(Optional.empty());

        mvc.perform(get("/api/v1/whiskies/101"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.detail").value("위스키를 찾을 수 없습니다."))
            .andExpect(jsonPath("$.instance").value("/api/v1/whiskies/101"));
    }

    @Test
    void rejectsNonNumericWhiskyId() throws Exception {
        mvc.perform(get("/api/v1/whiskies/abc"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.detail").value("위스키 ID가 올바르지 않습니다."));
    }

    private Whisky whisky(Long id, String name) {
        Whisky whisky = mock(Whisky.class);
        when(whisky.id()).thenReturn(id);
        when(whisky.name()).thenReturn(name);
        return whisky;
    }

    private Whisky listedWhisky() {
        WhiskyCategory category = mock(WhiskyCategory.class);
        when(category.id()).thenReturn(1L);
        when(category.name()).thenReturn("싱글 몰트");
        WhiskyOrigin origin = mock(WhiskyOrigin.class);
        when(origin.id()).thenReturn(1L);
        when(origin.name()).thenReturn("스코틀랜드");
        WhiskyRegion region = mock(WhiskyRegion.class);
        when(region.id()).thenReturn(10L);
        when(region.name()).thenReturn("아일라");
        Whisky whisky = mock(Whisky.class);
        when(whisky.id()).thenReturn(101L);
        when(whisky.name()).thenReturn("Lagavulin 16");
        when(whisky.volumeMl()).thenReturn(700);
        when(whisky.abv()).thenReturn(new BigDecimal("43.0"));
        when(whisky.category()).thenReturn(category);
        when(whisky.origin()).thenReturn(origin);
        when(whisky.region()).thenReturn(region);
        return whisky;
    }

    private SaleProduct saleProduct(
        Long id,
        String retailerName,
        String retailerAddress,
        String countryCode,
        boolean dutyFree,
        String productUrl,
        Boolean soldOut) {
        Retailer retailer = mock(Retailer.class);
        when(retailer.name()).thenReturn(retailerName);
        when(retailer.address()).thenReturn(retailerAddress);
        when(retailer.countryCode()).thenReturn(countryCode);
        when(retailer.isDutyFree()).thenReturn(dutyFree);
        SaleProduct saleProduct = mock(SaleProduct.class);
        when(saleProduct.id()).thenReturn(id);
        when(saleProduct.retailer()).thenReturn(retailer);
        when(saleProduct.productUrl()).thenReturn(productUrl);
        when(saleProduct.isSoldOut()).thenReturn(soldOut);
        return saleProduct;
    }
}
