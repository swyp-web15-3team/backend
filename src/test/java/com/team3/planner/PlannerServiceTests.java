package com.team3.planner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import com.team3.planner.dto.AddPlannerItemsRequest;
import com.team3.planner.dto.AddPlannerItemsResponse;
import com.team3.whisky.PriceHistoryRepository;
import com.team3.whisky.Retailer;
import com.team3.whisky.SaleProduct;
import com.team3.whisky.SaleProductRepository;
import com.team3.whisky.Whisky;

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

    private PlannerService service;

    @BeforeEach
    void setUp() {
        service = new PlannerService(planners, items, saleProducts, prices);
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

    private SaleProduct product(Long id, boolean soldOut) {
        return product(id, whisky(), soldOut);
    }

    private SaleProduct product(Long id, Whisky whisky, boolean soldOut) {
        Retailer retailer = mock(Retailer.class);
        when(retailer.countryCode()).thenReturn("KR");
        SaleProduct product = mock(SaleProduct.class);
        when(product.id()).thenReturn(id);
        when(product.whisky()).thenReturn(whisky);
        when(product.retailer()).thenReturn(retailer);
        when(product.isSoldOut()).thenReturn(soldOut);
        return product;
    }

    private Whisky whisky() {
        Whisky whisky = mock(Whisky.class);
        when(whisky.id()).thenReturn(101L);
        return whisky;
    }
}
