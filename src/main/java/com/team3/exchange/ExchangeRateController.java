package com.team3.exchange;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;

import com.team3.common.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class ExchangeRateController {
    private final ExchangeRateService service;

    public ExchangeRateController(ExchangeRateService service) {
        this.service = service;
    }

    @GetMapping("/api/v1/exchange-rates")
    public ApiResponse<ExchangeRateResponse> getRates(@RequestParam(required = false) String date) {
        LocalDate requested = LocalDate.now(ZoneId.of("Asia/Seoul"));
        if (date != null) {
            try {
                if (!date.matches("[0-9]{4}-[0-9]{2}-[0-9]{2}")) {
                    throw new DateTimeParseException("Invalid date format", date, 0);
                }
                requested = LocalDate.parse(date);
            } catch (DateTimeParseException ex) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Date must be a valid YYYY-MM-DD date.");
            }
        }
        return ApiResponse.of(ExchangeRateResponse.from(service.getRates(requested)));
    }
}
