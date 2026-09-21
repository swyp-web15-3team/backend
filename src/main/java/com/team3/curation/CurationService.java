package com.team3.curation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.team3.curation.dto.CurationListResponse;
import com.team3.curation.dto.CurationListResponse.CurationItem;
import com.team3.whisky.PriceHistoryRepository;
import com.team3.whisky.Whisky;
import com.team3.whisky.WhiskyLatestPrice;
import com.team3.whisky.WhiskyRepository;
import com.team3.whisky.dto.WhiskyListResponse.WhiskyItem;

import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CurationService {

    private static final int PREVIEW_LIMIT = 4;
    private static final Sort CURATION_SORT = Sort.by(Order.asc("id"));

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

    public CurationListResponse getCurations() {
        List<Curation> found = curations.findAll(CURATION_SORT);
        if (found.isEmpty()) {
            return new CurationListResponse(List.of());
        }
        List<Long> curationIds = found.stream().map(Curation::id).toList();
        Map<Long, List<Long>> previewIdsByCuration = previewWhiskyIds(curationIds);
        Map<Long, Whisky> whiskyById = loadWhiskies(previewIdsByCuration);
        Map<Long, WhiskyLatestPrice> lowestKr = new HashMap<>();
        Map<Long, WhiskyLatestPrice> lowestJp = new HashMap<>();
        collectLowestPrices(previewIdsByCuration, lowestKr, lowestJp);
        List<CurationItem> items = new ArrayList<>();
        for (Curation curation : found) {
            items.add(toItem(curation, previewIdsByCuration, whiskyById, lowestKr, lowestJp));
        }
        return new CurationListResponse(items);
    }

    private Map<Long, List<Long>> previewWhiskyIds(List<Long> curationIds) {
        Map<Long, List<Long>> previewIdsByCuration = new LinkedHashMap<>();
        for (Long curationId : curationIds) {
            previewIdsByCuration.put(curationId, new ArrayList<>());
        }
        for (CurationWhisky member : members.findByCurationIdInOrderByIdAsc(curationIds)) {
            List<Long> previewIds = previewIdsByCuration.get(member.curationId());
            if (previewIds == null || previewIds.size() >= PREVIEW_LIMIT) {
                continue;
            }
            if (previewIds.contains(member.whiskyId())) {
                continue;
            }
            previewIds.add(member.whiskyId());
        }
        return previewIdsByCuration;
    }

    private Map<Long, Whisky> loadWhiskies(Map<Long, List<Long>> previewIdsByCuration) {
        List<Long> whiskyIds = previewIds(previewIdsByCuration);
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
        Map<Long, List<Long>> previewIdsByCuration,
        Map<Long, WhiskyLatestPrice> lowestKr,
        Map<Long, WhiskyLatestPrice> lowestJp) {
        List<Long> whiskyIds = previewIds(previewIdsByCuration);
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

    private static List<Long> previewIds(Map<Long, List<Long>> previewIdsByCuration) {
        return previewIdsByCuration.values().stream()
            .flatMap(List::stream)
            .distinct()
            .toList();
    }

    private static CurationItem toItem(
        Curation curation,
        Map<Long, List<Long>> previewIdsByCuration,
        Map<Long, Whisky> whiskyById,
        Map<Long, WhiskyLatestPrice> lowestKr,
        Map<Long, WhiskyLatestPrice> lowestJp) {
        List<WhiskyItem> cards = new ArrayList<>();
        for (Long whiskyId : previewIdsByCuration.getOrDefault(curation.id(), List.of())) {
            Whisky whisky = whiskyById.get(whiskyId);
            if (whisky == null) {
                continue;
            }
            cards.add(WhiskyItem.from(whisky, lowestKr.get(whiskyId), lowestJp.get(whiskyId)));
        }
        return new CurationItem(curation.id(), curation.title(), cards);
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
}
