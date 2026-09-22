package com.team3.exchange;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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
        if (date.getYear() < 1 || date.getYear() > 9999 || date.isAfter(LocalDate.now(ZoneId.of("Asia/Seoul")))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Date must not be in the future.");
        }
        Optional<ExchangeRateSnapshot> existing = repository.findById(date);
        if (existing.isPresent()) {
            return existing.get();
        }
        // Transaction-scoped, per-date lock also covers requests from other application
        // instances.
        repository.lockDate(Math.toIntExact(date.toEpochDay()));
        return repository.findById(date)
            .orElseGet(() -> repository.save(new ExchangeRateSnapshot(date, client.fetch(date))));
    }
}
