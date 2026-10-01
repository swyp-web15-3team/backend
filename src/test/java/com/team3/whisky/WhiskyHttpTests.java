package com.team3.whisky;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import com.team3.security.SecurityConfig;
import com.team3.exchange.ExchangeRateService;
import com.team3.exchange.ExchangeRateSnapshot;
import com.team3.exchange.exception.ExchangeRateNotFoundException;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;
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
    private ExchangeRateService exchangeRates;

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
        when(lagavulin16.imageUrl()).thenReturn("https://example.com/whisky.jpg");
        whenSearchReturns(new PageImpl<>(List.of(lagavulin16), PageRequest.of(0, 20), 1));
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
            .andExpect(jsonPath("$.data.content[0].imageUrl").value("https://example.com/whisky.jpg"))
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
        whenSearchReturns(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mvc.perform(get("/api/v1/whiskies"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.content").isEmpty())
            .andExpect(jsonPath("$.data.totalElements").value(0))
            .andExpect(jsonPath("$.data.totalPages").value(0));
    }

    @Test
    void returnsMatchingWhiskiesForQuery() throws Exception {
        Whisky lagavulin16 = listedWhisky();
        whenSearchReturns(new PageImpl<>(List.of(lagavulin16), PageRequest.of(0, 20), 1));
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
    void rejectsMalformedSearchNumbersWithoutEchoingRejectedValues() throws Exception {
        mvc.perform(get("/api/v1/whiskies").param("size", "private-text"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.length()").value(1))
            .andExpect(jsonPath("$.errors[0].field").value("size"))
            .andExpect(jsonPath("$.errors[0].message").value("Invalid value."))
            .andExpect(jsonPath("$.errors[0].rejectedValue").doesNotExist())
            .andExpect(content().string(not(containsString("private-text"))));

        mvc.perform(get("/api/v1/whiskies").param("categoryId", "private-text"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.errors.length()").value(1))
            .andExpect(jsonPath("$.errors[0].field").value("categoryId"))
            .andExpect(jsonPath("$.errors[0].message").value("Invalid value."))
            .andExpect(jsonPath("$.errors[0].rejectedValue").doesNotExist())
            .andExpect(content().string(not(containsString("private-text"))));
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
    void acceptsRepeatedCategoriesAndDeduplicatesValidatedIds() throws Exception {
        when(categories.existsById(2L)).thenReturn(true);
        when(categories.existsById(3L)).thenReturn(true);
        whenSearchReturns(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mvc.perform(get("/api/v1/whiskies").param("categoryId", "2", "2", "3"));

        verify(whiskies).search(isNull(), eq(List.of(2L, 3L)), isNull(), isNull(), isNull(), isNull(), isNull(),
            isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class));
    }

    @Test
    void acceptsSingleCategoryId() throws Exception {
        when(categories.existsById(2L)).thenReturn(true);
        whenSearchReturns(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mvc.perform(get("/api/v1/whiskies").param("categoryId", "2"));

        verify(whiskies).search(isNull(), eq(List.of(2L)), isNull(), isNull(), isNull(), isNull(), isNull(),
            isNull(), isNull(), isNull(), isNull(), isNull(), any(Pageable.class));
    }

    @Test
    void passesPriceFiltersAndNormalizedYenRateToSearch() throws Exception {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        ExchangeRateSnapshot snapshot = new ExchangeRateSnapshot(today,
            new ObjectMapper().readTree("[{\"cur_unit\":\"JPY(100)\",\"deal_bas_r\":\"959.00\"}]"));
        when(exchangeRates.getRates(today)).thenReturn(snapshot);
        whenSearchReturns(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mvc.perform(get("/api/v1/whiskies").param("minPrice", "10000").param("maxPrice", "50000")
            .param("minPriceDiffPercent", "10").param("maxPriceDiffPercent", "20"));

        verify(whiskies).search(isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
            eq(new BigDecimal("10000")), eq(new BigDecimal("50000")), eq(new BigDecimal("10")),
            eq(new BigDecimal("20")), eq(new BigDecimal("9.5900")), any(Pageable.class));
        verify(exchangeRates).getRates(today);
    }

    @Test
    void usesYesterdayRateWhenTodaysRateIsNotFound() throws Exception {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        when(exchangeRates.getRates(today)).thenThrow(new ExchangeRateNotFoundException());
        when(exchangeRates.getRates(today.minusDays(1))).thenReturn(new ExchangeRateSnapshot(today.minusDays(1),
            new ObjectMapper().readTree("[{\"cur_unit\":\"JPY(100)\",\"deal_bas_r\":\"959.00\"}]")));
        whenSearchReturns(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        mvc.perform(get("/api/v1/whiskies").param("minPrice", "10000"))
            .andExpect(status().isOk());

        InOrder order = Mockito.inOrder(exchangeRates);
        order.verify(exchangeRates).getRates(today);
        order.verify(exchangeRates).getRates(today.minusDays(1));
        verify(whiskies).search(isNull(), isNull(), isNull(), isNull(), isNull(), isNull(), isNull(),
            eq(new BigDecimal("10000")), isNull(), isNull(), isNull(), eq(new BigDecimal("9.5900")),
            any(Pageable.class));
        verifyNoMoreInteractions(exchangeRates);
    }

    @Test
    void returnsUnavailableAfterSevenMissingRateDates() throws Exception {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        when(exchangeRates.getRates(any(LocalDate.class))).thenThrow(new ExchangeRateNotFoundException());

        mvc.perform(get("/api/v1/whiskies").param("minPrice", "10000"))
            .andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.detail").value("오늘의 엔화 환율 정보를 사용할 수 없습니다."));

        InOrder order = Mockito.inOrder(exchangeRates);
        for (int daysBack = 0; daysBack < 7; daysBack++) {
            order.verify(exchangeRates).getRates(today.minusDays(daysBack));
        }
        verify(exchangeRates, times(7)).getRates(any(LocalDate.class));
        verifyNoMoreInteractions(exchangeRates);
    }

    @Test
    void rejectsInvalidPriceRangesWithoutSearching() throws Exception {
        mvc.perform(get("/api/v1/whiskies").param("minPrice", "20").param("maxPrice", "10"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/whiskies").param("minPriceDiffPercent", "20")
            .param("maxPriceDiffPercent", "20"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void returnsGenericUnavailableForInvalidRateAndProviderFailure() throws Exception {
        LocalDate today = LocalDate.now(ZoneId.of("Asia/Seoul"));
        when(exchangeRates.getRates(today)).thenReturn(new ExchangeRateSnapshot(today,
            new ObjectMapper().readTree("[{\"cur_unit\":\"USD\",\"deal_bas_r\":\"1350.00\"}]")))
            .thenThrow(new com.team3.exchange.exception.ExchangeRateProviderException());

        mvc.perform(get("/api/v1/whiskies").param("minPrice", "10000"))
            .andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.detail").value("오늘의 엔화 환율 정보를 사용할 수 없습니다."));
        mvc.perform(get("/api/v1/whiskies").param("minPrice", "10000"))
            .andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.detail").value("오늘의 엔화 환율 정보를 사용할 수 없습니다."));
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
        when(whisky.imageUrl()).thenReturn("https://example.com/whisky.jpg");
        when(whiskies.findById(101L)).thenReturn(Optional.of(whisky));
        SaleProduct krProduct = saleProduct(
            501L, "롯데면세점", "서울특별시 중구 을지로 30", "KR", true, "https://example.com/product/501", false);
        when(krProduct.imageUrl()).thenReturn("https://example.com/retailer.jpg");
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
            .andExpect(jsonPath("$.data.imageUrl").value("https://example.com/whisky.jpg"))
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
            .andExpect(jsonPath("$.data.saleProducts[0].imageUrl").value("https://example.com/retailer.jpg"))
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
            .andExpect(jsonPath("$.data.saleProducts[1].imageUrl").value(org.hamcrest.Matchers.nullValue()))
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
            .andExpect(jsonPath("$.data.imageUrl").value(org.hamcrest.Matchers.nullValue()))
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

    @Test
    void returnsRelatedWhiskiesWithoutAuthentication() throws Exception {
        Whisky source = listedWhisky();
        Whisky related = whiskyCard(102L, "Lagavulin 8", new BigDecimal("48.0"));
        when(whiskies.findById(101L)).thenReturn(Optional.of(source));
        when(related.imageUrl()).thenReturn("https://example.com/related.jpg");
        when(whiskies.findRelated(101L, 1L, null, 10)).thenReturn(List.of(related));
        when(prices.findLatestAvailablePrices(List.of(102L))).thenReturn(List.of(
            new WhiskyLatestPrice(
                102L, new BigDecimal("120000"), "KRW", "KR", "롯데면세점", COLLECTED_AT, 1L),
            new WhiskyLatestPrice(
                102L, new BigDecimal("6800"), "JPY", "JP", "나리타 면세", COLLECTED_AT, 2L)));

        mvc.perform(get("/api/v1/whiskies/101/related"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").doesNotExist())
            .andExpect(jsonPath("$.data.whiskies.length()").value(1))
            .andExpect(jsonPath("$.data.whiskies[0].id").value(102))
            .andExpect(jsonPath("$.data.whiskies[0].name").value("Lagavulin 8"))
            .andExpect(jsonPath("$.data.whiskies[0].imageUrl").value("https://example.com/related.jpg"))
            .andExpect(jsonPath("$.data.whiskies[0].volumeMl").value(700))
            .andExpect(jsonPath("$.data.whiskies[0].abv").value(48.0))
            .andExpect(jsonPath("$.data.whiskies[0].category.id").value(1))
            .andExpect(jsonPath("$.data.whiskies[0].category.name").value("싱글 몰트"))
            .andExpect(jsonPath("$.data.whiskies[0].kr.amount").value(120000))
            .andExpect(jsonPath("$.data.whiskies[0].kr.currency").value("KRW"))
            .andExpect(jsonPath("$.data.whiskies[0].kr.retailerName").value("롯데면세점"))
            .andExpect(jsonPath("$.data.whiskies[0].kr.collectedAt").value("2026-09-07T18:00:00Z"))
            .andExpect(jsonPath("$.data.whiskies[0].kr.stale").value(false))
            .andExpect(jsonPath("$.data.whiskies[0].jp.amount").value(6800))
            .andExpect(jsonPath("$.data.whiskies[0].jp.currency").value("JPY"))
            .andExpect(jsonPath("$.data.whiskies[0].jp.amountKrw").isEmpty())
            .andExpect(jsonPath("$.data.whiskies[0].jp.retailerName").value("나리타 면세"))
            .andExpect(jsonPath("$.data.whiskies[0].jp.stale").value(false))
            .andExpect(jsonPath("$.data.whiskies[0].comparison").isEmpty());
        verify(whiskies, never()).findRelated(101L, null, 1L, 10);
    }

    @Test
    void returnsRelatedByOriginWhenCategoryIsEmpty() throws Exception {
        Whisky source = listedWhisky();
        Whisky related = whiskyCard(103L, "Talisker 10", new BigDecimal("45.8"));
        when(whiskies.findById(101L)).thenReturn(Optional.of(source));
        when(whiskies.findRelated(101L, 1L, null, 10)).thenReturn(List.of());
        when(whiskies.findRelated(101L, null, 1L, 10)).thenReturn(List.of(related));
        when(prices.findLatestAvailablePrices(List.of(103L))).thenReturn(List.of());

        mvc.perform(get("/api/v1/whiskies/101/related"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.whiskies.length()").value(1))
            .andExpect(jsonPath("$.data.whiskies[0].id").value(103))
            .andExpect(jsonPath("$.data.whiskies[0].name").value("Talisker 10"))
            .andExpect(jsonPath("$.data.whiskies[0].kr").isEmpty())
            .andExpect(jsonPath("$.data.whiskies[0].jp").isEmpty());
    }

    @Test
    void returnsEmptyRelatedWhiskiesWhenNoneExist() throws Exception {
        Whisky source = listedWhisky();
        when(whiskies.findById(101L)).thenReturn(Optional.of(source));
        when(whiskies.findRelated(101L, 1L, null, 10)).thenReturn(List.of());
        when(whiskies.findRelated(101L, null, 1L, 10)).thenReturn(List.of());

        mvc.perform(get("/api/v1/whiskies/101/related"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.whiskies").isEmpty());
    }

    @Test
    void rejectsUnknownWhiskyForRelated() throws Exception {
        when(whiskies.findById(101L)).thenReturn(Optional.empty());

        mvc.perform(get("/api/v1/whiskies/101/related"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.detail").value("위스키를 찾을 수 없습니다."))
            .andExpect(jsonPath("$.instance").value("/api/v1/whiskies/101/related"));
    }

    @Test
    void rejectsNonNumericWhiskyIdForRelated() throws Exception {
        mvc.perform(get("/api/v1/whiskies/abc/related"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.detail").value("위스키 ID가 올바르지 않습니다."));
    }

    private void whenSearchReturns(PageImpl<Whisky> page) {
        when(whiskies.search(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
            any(Pageable.class))).thenReturn(page);
    }

    private Whisky whisky(Long id, String name) {
        Whisky whisky = mock(Whisky.class);
        when(whisky.id()).thenReturn(id);
        when(whisky.name()).thenReturn(name);
        return whisky;
    }

    private Whisky listedWhisky() {
        return whiskyCard(101L, "Lagavulin 16", new BigDecimal("43.0"));
    }

    private Whisky whiskyCard(Long id, String name, BigDecimal abv) {
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
        when(whisky.id()).thenReturn(id);
        when(whisky.name()).thenReturn(name);
        when(whisky.volumeMl()).thenReturn(700);
        when(whisky.abv()).thenReturn(abv);
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
