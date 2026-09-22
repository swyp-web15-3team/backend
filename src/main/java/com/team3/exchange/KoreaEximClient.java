package com.team3.exchange;

import java.math.BigDecimal;
import java.net.http.HttpClient;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import com.team3.exchange.exception.ExchangeRateNotConfiguredException;
import com.team3.exchange.exception.ExchangeRateNotFoundException;
import com.team3.exchange.exception.ExchangeRateProviderException;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class KoreaEximClient {
    private final RestClient client;
    private final String apiKey;
    private final String apiUrl;

    @Autowired
    public KoreaEximClient(RestClient.Builder builder, @Value("${exchange.koreaexim.api-key:}") String apiKey,
        @Value("${exchange.koreaexim.api-url}") String apiUrl) {
        this(createClient(builder), apiKey, apiUrl);
    }

    KoreaEximClient(RestClient client, String apiKey, String apiUrl) {
        this.client = client;
        this.apiKey = apiKey;
        this.apiUrl = apiUrl;
    }

    private static RestClient createClient(RestClient.Builder builder) {
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2)).followRedirects(HttpClient.Redirect.NEVER).build());
        factory.setReadTimeout(Duration.ofSeconds(3));
        return builder.requestFactory(factory).build();
    }

    public JsonNode fetch(LocalDate date) {
        if (apiKey.isBlank()) {
            throw new ExchangeRateNotConfiguredException();
        }
        try {
            JsonNode rates = client.get().uri(apiUrl + "?authkey={key}&searchdate={date}&data=AP01",
                apiKey, date.format(DateTimeFormatter.BASIC_ISO_DATE)).retrieve().body(JsonNode.class);
            if (rates == null || rates.isNull() || (rates.isArray() && rates.isEmpty())) {
                throw new ExchangeRateNotFoundException();
            }
            if (!rates.isArray()) {
                throw new ExchangeRateProviderException();
            }
            Set<String> currencies = new HashSet<>();
            for (JsonNode rate : rates) {
                String currency = rate.path("cur_unit").asText();
                String baseRate = rate.path("deal_bas_r").asText();
                if (!rate.path("result").isInt() || rate.path("result").intValue() != 1
                    || !currency.matches("[A-Z]{3}(\\(100\\))?") || !currencies.add(currency)
                    || rate.path("cur_nm").asText().isBlank()
                    || !baseRate.matches("(?:[0-9]+|[0-9]{1,3}(?:,[0-9]{3})+)(?:\\.[0-9]+)?")
                    || new BigDecimal(baseRate.replace(",", "")).signum() <= 0) {
                    throw new ExchangeRateProviderException();
                }
            }
            return rates;
        } catch (RestClientException ex) {
            // Provider exceptions may contain the API key in the request URL.
            throw new ExchangeRateProviderException();
        }
    }
}
