package com.team3.whisky;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.team3.whisky.dto.WhiskyDetailResponse;
import com.team3.whisky.dto.WhiskyDetailResponse.SaleProductItem;
import com.team3.whisky.dto.WhiskyListResponse;
import com.team3.whisky.dto.WhiskyListResponse.WhiskyItem;
import com.team3.whisky.dto.WhiskyRelatedResponse;
import com.team3.whisky.dto.WhiskySuggestionsResponse;
import com.team3.whisky.dto.WhiskySuggestionsResponse.Suggestion;

import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class WhiskyService {

    private static final int MAX_QUERY_LENGTH = 255;
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 50;
    private static final Limit SUGGESTION_LIMIT = Limit.of(10);
    private static final Sort SUGGESTION_SORT = Sort.by("id").ascending();
    private static final int RELATED_LIMIT = 10;

    private final WhiskyRepository whiskies;
    private final WhiskyCategoryRepository categories;
    private final WhiskyOriginRepository origins;
    private final WhiskyRegionRepository regions;
    private final PriceHistoryRepository prices;
    private final SaleProductRepository saleProducts;

    public WhiskyService(
        WhiskyRepository whiskies,
        WhiskyCategoryRepository categories,
        WhiskyOriginRepository origins,
        WhiskyRegionRepository regions,
        PriceHistoryRepository prices,
        SaleProductRepository saleProducts) {
        this.whiskies = whiskies;
        this.categories = categories;
        this.origins = origins;
        this.regions = regions;
        this.prices = prices;
        this.saleProducts = saleProducts;
    }

    public WhiskySuggestionsResponse getSuggestions(String query) {
        List<Whisky> found;
        if (query == null) {
            found = whiskies.findAllBy(SUGGESTION_SORT, SUGGESTION_LIMIT);
        } else {
            found = whiskies.findByNameContaining(keyword(query), SUGGESTION_SORT, SUGGESTION_LIMIT);
        }
        List<Suggestion> suggestions = found.stream()
            .map(whisky -> new Suggestion(whisky.name()))
            .toList();
        return new WhiskySuggestionsResponse(suggestions);
    }

    public WhiskyListResponse getWhiskies(
        String query,
        Long categoryId,
        Long originId,
        Long regionId,
        Integer volumeMl,
        String countryCode,
        Boolean isDutyFree,
        String sort,
        Integer page,
        Integer size) {
        String keyword = query == null ? null : keyword(query);
        int pageNumber = pageNumber(page);
        int pageSize = pageSize(size);
        String saleCountry = saleCountry(countryCode);
        validateFilters(categoryId, originId, regionId);
        Page<Whisky> found = whiskies.search(
            keyword,
            categoryId,
            originId,
            regionId,
            volumeMl,
            saleCountry,
            isDutyFree,
            PageRequest.of(pageNumber, pageSize, listSort(sort)));
        Map<Long, WhiskyLatestPrice> lowestKr = new HashMap<>();
        Map<Long, WhiskyLatestPrice> lowestJp = new HashMap<>();
        collectLowestPrices(found.getContent(), lowestKr, lowestJp);
        List<WhiskyItem> content = found.getContent().stream()
            .map(whisky -> WhiskyItem.from(whisky, lowestKr.get(whisky.id()), lowestJp.get(whisky.id())))
            .toList();
        return new WhiskyListResponse(content, pageNumber, pageSize, found.getTotalElements(), found.getTotalPages());
    }

    public WhiskyDetailResponse getWhisky(Long whiskyId) {
        Whisky whisky = whiskies.findById(whiskyId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "위스키를 찾을 수 없습니다."));
        List<SaleProduct> foundSaleProducts = saleProducts.findByWhiskyIdOrderByIdAsc(whiskyId);
        Map<Long, WhiskyLatestPrice> latestBySaleProduct = latestPrices(foundSaleProducts);
        Map<Long, WhiskyLatestPrice> lowestKr = new HashMap<>();
        Map<Long, WhiskyLatestPrice> lowestJp = new HashMap<>();
        collectLowestPrices(List.of(whisky), lowestKr, lowestJp);
        List<SaleProductItem> items = foundSaleProducts.stream()
            .map(saleProduct -> SaleProductItem.from(saleProduct, latestBySaleProduct.get(saleProduct.id())))
            .toList();
        return WhiskyDetailResponse.from(whisky, lowestKr.get(whiskyId), lowestJp.get(whiskyId), items);
    }

    public WhiskyRelatedResponse getRelated(Long whiskyId) {
        Whisky whisky = whiskies.findById(whiskyId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "위스키를 찾을 수 없습니다."));
        Long categoryId = whisky.category() == null ? null : whisky.category().id();
        Long originId = whisky.origin() == null ? null : whisky.origin().id();
        List<Whisky> related = List.of();
        if (categoryId != null) {
            related = whiskies.findRelated(whiskyId, categoryId, null, RELATED_LIMIT);
        }
        if (related.isEmpty() && originId != null) {
            related = whiskies.findRelated(whiskyId, null, originId, RELATED_LIMIT);
        }
        Map<Long, WhiskyLatestPrice> lowestKr = new HashMap<>();
        Map<Long, WhiskyLatestPrice> lowestJp = new HashMap<>();
        collectLowestPrices(related, lowestKr, lowestJp);
        List<WhiskyItem> items = related.stream()
            .map(item -> WhiskyItem.from(item, lowestKr.get(item.id()), lowestJp.get(item.id())))
            .toList();
        return new WhiskyRelatedResponse(items);
    }

    private static String keyword(String query) {
        String keyword = query.trim();
        if (keyword.isEmpty() || keyword.length() > MAX_QUERY_LENGTH) {
            throw badRequest("검색어가 올바르지 않습니다.");
        }
        return keyword;
    }

    private static int pageNumber(Integer page) {
        if (page == null) {
            return DEFAULT_PAGE;
        }
        if (page < DEFAULT_PAGE) {
            throw badRequest("size는 1 이상 50 이하여야 합니다.");
        }
        return page;
    }

    private static int pageSize(Integer size) {
        if (size == null) {
            return DEFAULT_PAGE_SIZE;
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw badRequest("size는 1 이상 50 이하여야 합니다.");
        }
        return size;
    }

    private static String saleCountry(String countryCode) {
        if (countryCode == null) {
            return null;
        }
        String code = countryCode.trim();
        if (!"KR".equals(code) && !"JP".equals(code)) {
            throw badRequest("판매 국가는 KR 또는 JP여야 합니다.");
        }
        return code;
    }

    private static Sort listSort(String sort) {
        if (sort == null || sort.isBlank() || "name,asc".equals(sort)) {
            return Sort.by(Order.asc("name"), Order.asc("id"));
        }
        if ("name,desc".equals(sort)) {
            return Sort.by(Order.desc("name"), Order.asc("id"));
        }
        if ("id,asc".equals(sort)) {
            return Sort.by(Order.asc("id"));
        }
        if ("id,desc".equals(sort)) {
            return Sort.by(Order.desc("id"));
        }
        throw badRequest("지원하지 않는 정렬 조건입니다.");
    }

    private void validateFilters(Long categoryId, Long originId, Long regionId) {
        if (categoryId != null && !categories.existsById(categoryId)) {
            throw badRequest("존재하지 않는 필터 값입니다.");
        }
        if (originId != null && !origins.existsById(originId)) {
            throw badRequest("존재하지 않는 필터 값입니다.");
        }
        if (regionId == null) {
            return;
        }
        WhiskyRegion region = regions.findById(regionId)
            .orElseThrow(() -> badRequest("존재하지 않는 필터 값입니다."));
        if (originId != null && !originId.equals(region.origin().id())) {
            throw badRequest("생산 지역과 원산지가 일치하지 않습니다.");
        }
    }

    private Map<Long, WhiskyLatestPrice> latestPrices(List<SaleProduct> foundSaleProducts) {
        if (foundSaleProducts.isEmpty()) {
            return Map.of();
        }
        List<Long> saleProductIds = foundSaleProducts.stream().map(SaleProduct::id).toList();
        Map<Long, WhiskyLatestPrice> latestBySaleProduct = new HashMap<>();
        for (WhiskyLatestPrice price : prices.findLatestPrices(saleProductIds)) {
            latestBySaleProduct.put(price.saleProductId(), price);
        }
        return latestBySaleProduct;
    }

    private void collectLowestPrices(
        List<Whisky> found,
        Map<Long, WhiskyLatestPrice> lowestKr,
        Map<Long, WhiskyLatestPrice> lowestJp) {
        if (found.isEmpty()) {
            return;
        }
        List<Long> whiskyIds = found.stream().map(Whisky::id).toList();
        for (WhiskyLatestPrice price : prices.findLatestAvailablePrices(whiskyIds)) {
            if ("KR".equals(price.countryCode()) && "KRW".equals(price.currencyCode())) {
                lowestKr.merge(price.whiskyId(), price, WhiskyService::lowerPrice);
            } else if ("JP".equals(price.countryCode()) && "JPY".equals(price.currencyCode())) {
                lowestJp.merge(price.whiskyId(), price, WhiskyService::lowerPrice);
            }
        }
    }

    private static WhiskyLatestPrice lowerPrice(WhiskyLatestPrice left, WhiskyLatestPrice right) {
        int compared = left.amount().compareTo(right.amount());
        if (compared < 0) {
            return left;
        }
        if (compared > 0) {
            return right;
        }
        if (left.saleProductId() <= right.saleProductId()) {
            return left;
        }
        return right;
    }

    private static ResponseStatusException badRequest(String detail) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, detail);
    }
}
