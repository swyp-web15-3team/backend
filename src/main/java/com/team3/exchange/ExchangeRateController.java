package com.team3.exchange;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;

import com.team3.common.ApiResponse;
import com.team3.exchange.exception.InvalidExchangeRateDateException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/exchange-rates")
public class ExchangeRateController {
    private final ExchangeRateService service;

    public ExchangeRateController(ExchangeRateService service) {
        this.service = service;
    }

    @GetMapping
    public ApiResponse<ExchangeRateResponse> getRates(@RequestParam(required = false) String date) {
        LocalDate requested;
        try {
            requested = date == null ? LocalDate.now(ZoneId.of("Asia/Seoul")) : LocalDate.parse(date);
        } catch (DateTimeParseException ex) {
            throw new InvalidExchangeRateDateException();
        }
        return ApiResponse.of(ExchangeRateResponse.from(service.getRates(requested)));
    }
}
