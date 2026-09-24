package com.team3.exchange;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import com.team3.exchange.exception.InvalidExchangeRateDateException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExchangeRateService {
    private final ExchangeRateRepository repository;
    private final KoreaEximClient client;

    public ExchangeRateService(ExchangeRateRepository repository, KoreaEximClient client) {
        this.repository = repository;
        this.client = client;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public ExchangeRateSnapshot getRates(LocalDate date) {
        if (date.isAfter(LocalDate.now(ZoneId.of("Asia/Seoul")))) {
            throw new InvalidExchangeRateDateException();
        }
        Optional<ExchangeRateSnapshot> existing = repository.findById(date);
        if (existing.isPresent()) {
            return existing.get();
        }

        repository.lockDate(Math.toIntExact(date.toEpochDay()));
        return repository.findById(date)
            .orElseGet(() -> repository.save(new ExchangeRateSnapshot(date, client.fetch(date))));
    }
}
