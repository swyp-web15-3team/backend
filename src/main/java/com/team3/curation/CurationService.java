package com.team3.curation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.team3.curation.dto.CurationListResponse;
import com.team3.whisky.PriceHistoryRepository;
import com.team3.whisky.Whisky;
import com.team3.whisky.WhiskyLatestPrice;
import com.team3.whisky.WhiskyRepository;
import com.team3.whisky.dto.WhiskyListResponse.WhiskyItem;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class CurationService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 50;

    private final CurationRepository curations;
    private final CurationWhiskyRepository members;
    private final WhiskyRepository whiskies;
    private final PriceHistoryRepository prices;

    public CurationService(
        CurationRepository curations,
        CurationWhiskyRepository members,
        WhiskyRepository whiskies,
        PriceHistoryRepository prices) {
        this.curations = curations;
        this.members = members;
        this.whiskies = whiskies;
        this.prices = prices;
    }

    public CurationListResponse getCurations(Integer page, Integer size) {
        int pageNumber = pageNumber(page);
        int pageSize = pageSize(size);
        Optional<Curation> found = curations.findFirstByOrderByIdAsc();
        if (found.isEmpty()) {
            return CurationListResponse.empty(pageNumber, pageSize);
        }
        Curation curation = found.get();
        Page<CurationWhisky> memberPage = members.findByCurationIdOrderByIdAsc(
            curation.id(), PageRequest.of(pageNumber, pageSize));
        List<Long> whiskyIds = new ArrayList<>();
        for (CurationWhisky member : memberPage.getContent()) {
            if (!whiskyIds.contains(member.whiskyId())) {
                whiskyIds.add(member.whiskyId());
            }
        }
        Map<Long, Whisky> whiskyById = loadWhiskies(whiskyIds);
        Map<Long, WhiskyLatestPrice> lowestKr = new HashMap<>();
        Map<Long, WhiskyLatestPrice> lowestJp = new HashMap<>();
        collectLowestPrices(whiskyIds, lowestKr, lowestJp);
        List<WhiskyItem> content = new ArrayList<>();
        for (Long whiskyId : whiskyIds) {
            Whisky whisky = whiskyById.get(whiskyId);
            if (whisky == null) {
                continue;
            }
            content.add(WhiskyItem.from(whisky, lowestKr.get(whiskyId), lowestJp.get(whiskyId)));
        }
        return new CurationListResponse(
            curation.id(),
            curation.title(),
            content,
            pageNumber,
            pageSize,
            memberPage.getTotalElements(),
            memberPage.getTotalPages());
    }

    private Map<Long, Whisky> loadWhiskies(List<Long> whiskyIds) {
        if (whiskyIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, Whisky> whiskyById = new HashMap<>();
        for (Whisky whisky : whiskies.findByIdIn(whiskyIds)) {
            whiskyById.put(whisky.id(), whisky);
        }
        return whiskyById;
    }

    private void collectLowestPrices(
        List<Long> whiskyIds,
        Map<Long, WhiskyLatestPrice> lowestKr,
        Map<Long, WhiskyLatestPrice> lowestJp) {
        if (whiskyIds.isEmpty()) {
            return;
        }
        for (WhiskyLatestPrice price : prices.findLatestAvailablePrices(whiskyIds)) {
            if ("KR".equals(price.countryCode()) && "KRW".equals(price.currencyCode())) {
                lowestKr.merge(price.whiskyId(), price, CurationService::lowerPrice);
            } else if ("JP".equals(price.countryCode()) && "JPY".equals(price.currencyCode())) {
                lowestJp.merge(price.whiskyId(), price, CurationService::lowerPrice);
            }
        }
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
