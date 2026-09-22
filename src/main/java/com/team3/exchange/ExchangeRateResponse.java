package com.team3.exchange;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.JsonNode;

public record ExchangeRateResponse(
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd") LocalDate date,
    List<Rate> rates) {

    public static ExchangeRateResponse from(ExchangeRateSnapshot snapshot) {
        List<Rate> rates = new ArrayList<>();
        for (JsonNode source : snapshot.getRates()) {
            String currency = source.get("cur_unit").asText();
            BigDecimal rate = new BigDecimal(source.get("deal_bas_r").asText().replace(",", ""));
            if (currency.endsWith("(100)")) {
                currency = currency.substring(0, 3);
                rate = rate.movePointLeft(2);
            }
            rates.add(new Rate(currency, rate));
        }
        return new ExchangeRateResponse(snapshot.getDate(), rates);
    }

    public record Rate(String currency, BigDecimal rate) {
    }
}
