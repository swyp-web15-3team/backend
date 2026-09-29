package com.team3.planner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.team3.exchange.ExchangeRateRepository;
import com.team3.exchange.ExchangeRateSnapshot;
import com.team3.planner.dto.AddPlannerItemsRequest;
import com.team3.planner.dto.AddPlannerItemsResponse;
import com.team3.planner.dto.PlannerResponse;
import com.team3.whisky.PriceHistoryRepository;
import com.team3.whisky.Retailer;
import com.team3.whisky.SaleProduct;
import com.team3.whisky.SaleProductRepository;
import com.team3.whisky.Whisky;
import com.team3.whisky.WhiskyCategory;
import com.team3.whisky.WhiskyLatestPrice;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PlannerServiceTests {

    @Mock
    private PlannerRepository planners;

    @Mock
    private PlannerItemRepository items;

    @Mock
    private SaleProductRepository saleProducts;

    @Mock
    private PriceHistoryRepository prices;

    @Mock
    private ExchangeRateRepository exchangeRates;

    private PlannerService service;

    @BeforeEach
    void setUp() {
        service = new PlannerService(planners, items, saleProducts, prices, exchangeRates);
    }

    @Test
    void addsDomesticSoldOutProductWithoutPrice() {
        Planner planner = mock(Planner.class);
        when(planner.id()).thenReturn(10L);
        when(planners.findByUserId(42L)).thenReturn(Optional.of(planner));
        SaleProduct product = product(43L, true);
        when(saleProducts.findByIdIn(List.of(43L))).thenReturn(List.of(product));
        when(prices.findLatestPrices(List.of(43L))).thenReturn(List.of());
        when(items.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        AddPlannerItemsResponse response = service.addItems(
            42L,
            new AddPlannerItemsRequest(
                List.of(new AddPlannerItemsRequest.Item(43L, null, null))));

        assertThat(response.items()).hasSize(1);
        AddPlannerItemsResponse.PlannerItemResponse item = response.items().getFirst();
        assertThat(item.countryCode()).isEqualTo("KR");
        assertThat(item.isSoldOut()).isTrue();
        assertThat(item.price()).isNull();
        assertThat(item.computable()).isFalse();
    }

    @Test
    void convertsYenPriceWithTodaysExchangeRate() {
        Planner planner = mock(Planner.class);
        when(planner.id()).thenReturn(10L);
        when(planners.findByUserId(42L)).thenReturn(Optional.of(planner));
        SaleProduct product = product(501L, false, "JP");
        when(saleProducts.findByIdIn(List.of(501L))).thenReturn(List.of(product));
        when(prices.findLatestPrices(List.of(501L))).thenReturn(List.of(
            new WhiskyLatestPrice(
                101L, new BigDecimal("9800"), "JPY", "JP", "나리타 면세", Instant.now(), 501L)));
        when(exchangeRates.findById(any(LocalDate.class))).thenReturn(Optional.of(exchangeRate("JPY(100)", "959")));
        when(items.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        AddPlannerItemsResponse response = service.addItems(
            42L,
            new AddPlannerItemsRequest(
                List.of(new AddPlannerItemsRequest.Item(501L, 1, "CANDIDATE"))));

        AddPlannerItemsResponse.PlannerItemResponse item = response.items().getFirst();
        assertThat(item.price().amountKrw()).isEqualByComparingTo("93982");
        assertThat(item.exchange().source()).isEqualTo("KOREA_EXIM");
        assertThat(item.exchange().krwPerJpy()).isEqualByComparingTo("9.59");
        assertThat(item.computable()).isTrue();
    }

    @Test
    void addsYenProductWhenExchangeRateIsUnavailable() {
        Planner planner = mock(Planner.class);
        when(planner.id()).thenReturn(10L);
        when(planners.findByUserId(42L)).thenReturn(Optional.of(planner));
        SaleProduct product = product(501L, false, "JP");
        when(saleProducts.findByIdIn(List.of(501L))).thenReturn(List.of(product));
        when(prices.findLatestPrices(List.of(501L))).thenReturn(List.of(
            new WhiskyLatestPrice(
                101L, new BigDecimal("9800"), "JPY", "JP", "나리타 면세", Instant.now(), 501L)));
        when(exchangeRates.findById(any(LocalDate.class))).thenReturn(Optional.empty());
        when(items.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        AddPlannerItemsResponse response = service.addItems(
            42L,
            new AddPlannerItemsRequest(
                List.of(new AddPlannerItemsRequest.Item(501L, 1, "CANDIDATE"))));

        AddPlannerItemsResponse.PlannerItemResponse item = response.items().getFirst();
        assertThat(item.price().amountKrw()).isNull();
        assertThat(item.exchange()).isNull();
        assertThat(item.computable()).isFalse();
    }

    @Test
    void includesWhiskyCategoryInPlannerItemResponse() {
        PlannerItem item = mock(PlannerItem.class);
        when(item.saleProductId()).thenReturn(43L);
        SaleProduct product = mock(SaleProduct.class);
        Whisky whisky = mock(Whisky.class);
        WhiskyCategory category = mock(WhiskyCategory.class);
        Retailer retailer = mock(Retailer.class);
        when(product.whisky()).thenReturn(whisky);
        when(product.retailer()).thenReturn(retailer);
        when(whisky.category()).thenReturn(category);
        when(category.id()).thenReturn(1L);
        when(category.name()).thenReturn("싱글 몰트");

        PlannerResponse response = PlannerResponse.from(
            List.of(item), Map.of(43L, product), Map.of(), null, null);

        assertThat(response.items().getFirst().category().id()).isEqualTo(1L);
        assertThat(response.items().getFirst().category().name()).isEqualTo("싱글 몰트");
    }

    @Test
    void convertsPlannerYenPriceWithTodaysExchangeRate() {
        Planner planner = mock(Planner.class);
        when(planner.id()).thenReturn(10L);
        when(planners.findByUserId(42L)).thenReturn(Optional.of(planner));
        PlannerItem plannerItem = mock(PlannerItem.class);
        when(plannerItem.saleProductId()).thenReturn(501L);
        when(items.findByPlannerIdOrderByIdAsc(10L)).thenReturn(List.of(plannerItem));
        SaleProduct product = product(501L, false, "JP");
        when(saleProducts.findByIdIn(List.of(501L))).thenReturn(List.of(product));
        when(prices.findLatestPrices(List.of(501L))).thenReturn(List.of(
            new WhiskyLatestPrice(
                101L, new BigDecimal("9800"), "JPY", "JP", "나리타 면세", Instant.now(), 501L)));
        when(exchangeRates.findById(any(LocalDate.class))).thenReturn(Optional.of(exchangeRate("JPY(100)", "959")));

        PlannerResponse response = service.getPlanner(42L);

        PlannerResponse.PlannerListItemResponse item = response.items().getFirst();
        assertThat(item.price().amountKrw()).isEqualByComparingTo("93982");
        assertThat(item.exchange().krwPerJpy()).isEqualByComparingTo("9.59");
        assertThat(item.computable()).isTrue();
    }

    private SaleProduct product(Long id, boolean soldOut) {
        return product(id, soldOut, "KR");
    }

    private SaleProduct product(Long id, boolean soldOut, String countryCode) {
        Retailer retailer = mock(Retailer.class);
        when(retailer.countryCode()).thenReturn(countryCode);
        SaleProduct product = mock(SaleProduct.class);
        Whisky whisky = whisky();
        when(product.id()).thenReturn(id);
        when(product.whisky()).thenReturn(whisky);
        when(product.retailer()).thenReturn(retailer);
        when(product.isSoldOut()).thenReturn(soldOut);
        return product;
    }

    private ExchangeRateSnapshot exchangeRate(String currency, String rate) {
        ObjectMapper mapper = new ObjectMapper();
        ArrayNode rates = mapper.createArrayNode();
        ObjectNode item = rates.addObject();
        item.put("cur_unit", currency);
        item.put("deal_bas_r", rate);
        return new ExchangeRateSnapshot(LocalDate.now(), rates);
    }

    private Whisky whisky() {
        Whisky whisky = mock(Whisky.class);
        when(whisky.id()).thenReturn(101L);
        return whisky;
    }
}
