package com.team3.whisky;

import java.util.Collection;
import java.util.List;

public interface PriceHistoryRepositoryCustom {

    List<WhiskyLatestPrice> findLatestAvailablePrices(Collection<Long> whiskyIds);
}
