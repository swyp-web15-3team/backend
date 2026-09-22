package com.team3.planner;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.team3.common.exception.ErrorCode;
import com.team3.planner.dto.AddPlannerItemsRequest;
import com.team3.planner.dto.AddPlannerItemsRequest.Item;
import com.team3.planner.dto.AddPlannerItemsResponse;
import com.team3.planner.dto.AddPlannerItemsResponse.PlannerItemResponse;
import com.team3.planner.dto.PlannerResponse;
import com.team3.planner.exception.PlannerException;
import com.team3.whisky.PriceHistoryRepository;
import com.team3.whisky.SaleProduct;
import com.team3.whisky.SaleProductRepository;
import com.team3.whisky.WhiskyLatestPrice;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PlannerService {

    private static final int MAX_ITEM_KINDS = 20;
    private static final int MIN_QUANTITY = 1;
    private static final int MAX_QUANTITY = 20;
    private static final String JAPAN = "JP";
    private static final String YEN = "JPY";

    private final PlannerRepository planners;
    private final PlannerItemRepository items;
    private final SaleProductRepository saleProducts;
    private final PriceHistoryRepository prices;

    public PlannerService(
        PlannerRepository planners,
        PlannerItemRepository items,
        SaleProductRepository saleProducts,
        PriceHistoryRepository prices) {
        this.planners = planners;
        this.items = items;
        this.saleProducts = saleProducts;
        this.prices = prices;
    }

    public AddPlannerItemsResponse addItems(Long userId, AddPlannerItemsRequest request) {
        List<Item> requested = request == null ? List.of() : request.items();
        if (requested == null || requested.isEmpty()) {
            throw new PlannerException(ErrorCode.PLANNER_ITEMS_MISSING);
        }
        if (requested.size() > MAX_ITEM_KINDS) {
            throw new PlannerException(ErrorCode.PLANNER_ITEMS_LIMIT);
        }
        List<PreparedItem> prepared = prepare(requested);
        Planner planner = findOrCreatePlanner(userId);
        List<PlannerItem> created = items.saveAll(toRows(planner.id(), prepared));
        Map<Long, SaleProduct> products = new HashMap<>();
        Map<Long, WhiskyLatestPrice> latestPrices = new HashMap<>();
        for (PreparedItem item : prepared) {
            products.put(item.product().id(), item.product());
            latestPrices.put(item.product().id(), item.price());
        }
        return new AddPlannerItemsResponse(PlannerItemResponse.from(created, products, latestPrices));
    }

    @Transactional(readOnly = true)
    public PlannerResponse getPlanner(Long userId) {
        Optional<Planner> planner = planners.findByUserId(userId);
        if (planner.isEmpty()) {
            return PlannerResponse.empty();
        }
        List<PlannerItem> found = items.findByPlannerIdOrderByIdAsc(planner.get().id());
        if (found.isEmpty()) {
            return PlannerResponse.empty();
        }
        List<Long> saleProductIds = new ArrayList<>();
        for (PlannerItem item : found) {
            saleProductIds.add(item.saleProductId());
        }
        return PlannerResponse.from(found, loadProducts(saleProductIds), loadYenPrices(saleProductIds));
    }

    public void deleteItem(Long userId, Long plannerItemId) {
        PlannerItem item = items.findById(plannerItemId)
            .orElseThrow(() -> new PlannerException(ErrorCode.PLANNER_ITEM_NOT_FOUND));
        Planner planner = planners.findById(item.plannerId())
            .orElseThrow(() -> new PlannerException(ErrorCode.PLANNER_ITEM_NOT_FOUND));
        if (!userId.equals(planner.userId())) {
            throw new PlannerException(ErrorCode.PLANNER_ITEM_FORBIDDEN);
        }
        items.delete(item);
    }

    public void deleteItems(Long userId, String listType, Long saleProductId) {
        boolean hasListType = listType != null && !listType.isBlank();
        if (saleProductId != null && !hasListType) {
            throw new PlannerException(ErrorCode.PLANNER_LIST_TYPE_REQUIRED);
        }
        PlannerListType parsedListType = hasListType ? requiredListType(listType) : null;
        Optional<Planner> planner = planners.findByUserId(userId);
        if (planner.isEmpty()) {
            return;
        }
        Long plannerId = planner.get().id();
        if (parsedListType == null) {
            items.deleteByPlannerId(plannerId);
            return;
        }
        if (saleProductId == null) {
            items.deleteByPlannerIdAndListType(plannerId, parsedListType);
            return;
        }
        items.deleteByPlannerIdAndListTypeAndSaleProductId(plannerId, parsedListType, saleProductId);
    }

    private List<PreparedItem> prepare(List<Item> requested) {
        Set<Long> seen = new HashSet<>();
        List<Long> saleProductIds = new ArrayList<>();
        List<PlannerListType> listTypes = new ArrayList<>();
        List<Integer> quantities = new ArrayList<>();
        for (Item item : requested) {
            Long saleProductId = item == null ? null : item.saleProductId();
            if (saleProductId == null) {
                throw new PlannerException(ErrorCode.PLANNER_SALE_PRODUCT_ID_REQUIRED);
            }
            if (!seen.add(saleProductId)) {
                throw new PlannerException(ErrorCode.PLANNER_DUPLICATE_SALE_PRODUCT);
            }
            saleProductIds.add(saleProductId);
            quantities.add(quantity(item.quantity()));
            listTypes.add(listType(item.listType()));
        }
        Map<Long, SaleProduct> products = loadProducts(saleProductIds);
        Map<Long, WhiskyLatestPrice> latestPrices = loadYenPrices(saleProductIds);
        List<PreparedItem> prepared = new ArrayList<>();
        for (int index = 0; index < saleProductIds.size(); index++) {
            Long saleProductId = saleProductIds.get(index);
            SaleProduct product = products.get(saleProductId);
            if (product == null || product.whisky() == null) {
                throw new PlannerException(ErrorCode.SALE_PRODUCT_NOT_FOUND, saleProductId);
            }
            if (!JAPAN.equals(product.retailer().countryCode())) {
                throw new PlannerException(ErrorCode.PLANNER_NOT_JAPANESE, saleProductId);
            }
            if (Boolean.TRUE.equals(product.isSoldOut())) {
                throw new PlannerException(ErrorCode.PLANNER_SOLD_OUT, saleProductId);
            }
            if (product.isSoldOut() == null) {
                throw new PlannerException(ErrorCode.PLANNER_STOCK_UNKNOWN, saleProductId);
            }
            WhiskyLatestPrice price = latestPrices.get(saleProductId);
            if (price == null) {
                throw new PlannerException(ErrorCode.PLANNER_PRICE_MISSING, saleProductId);
            }
            prepared.add(new PreparedItem(listTypes.get(index), quantities.get(index), product, price));
        }
        return prepared;
    }

    private int quantity(Integer quantity) {
        int value = quantity == null ? MIN_QUANTITY : quantity;
        if (value < MIN_QUANTITY || value > MAX_QUANTITY) {
            throw new PlannerException(ErrorCode.PLANNER_INVALID_QUANTITY);
        }
        return value;
    }

    private PlannerListType listType(String listType) {
        if (listType == null || listType.isBlank()) {
            return PlannerListType.CANDIDATE;
        }
        return requiredListType(listType);
    }

    private PlannerListType requiredListType(String listType) {
        try {
            return PlannerListType.valueOf(listType);
        } catch (IllegalArgumentException ex) {
            throw new PlannerException(ErrorCode.PLANNER_INVALID_LIST_TYPE);
        }
    }

    private Map<Long, SaleProduct> loadProducts(List<Long> saleProductIds) {
        Map<Long, SaleProduct> products = new HashMap<>();
        for (SaleProduct product : saleProducts.findByIdIn(saleProductIds)) {
            products.put(product.id(), product);
        }
        return products;
    }

    private Map<Long, WhiskyLatestPrice> loadYenPrices(List<Long> saleProductIds) {
        Map<Long, WhiskyLatestPrice> latestPrices = new HashMap<>();
        for (WhiskyLatestPrice price : prices.findLatestPrices(saleProductIds)) {
            if (YEN.equals(price.currencyCode())) {
                latestPrices.put(price.saleProductId(), price);
            }
        }
        return latestPrices;
    }

    private Planner findOrCreatePlanner(Long userId) {
        Optional<Planner> existing = planners.findByUserId(userId);
        if (existing.isPresent()) {
            return existing.get();
        }
        try {
            return planners.saveAndFlush(new Planner(userId));
        } catch (DataIntegrityViolationException ex) {
            if (isUserConflict(ex)) {
                return planners.findByUserId(userId).orElseThrow(() -> ex);
            }
            throw ex;
        }
    }

    private boolean isUserConflict(Throwable exception) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof ConstraintViolationException violation
                && "uk_planners_user".equals(violation.getConstraintName())) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

    private List<PlannerItem> toRows(Long plannerId, List<PreparedItem> prepared) {
        List<PlannerItem> rows = new ArrayList<>();
        for (PreparedItem item : prepared) {
            for (int count = 0; count < item.quantity(); count++) {
                rows.add(new PlannerItem(plannerId, item.product().id(), item.listType()));
            }
        }
        return rows;
    }

    private record PreparedItem(
        PlannerListType listType,
        int quantity,
        SaleProduct product,
        WhiskyLatestPrice price) {
    }
}
