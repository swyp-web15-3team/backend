ALTER TABLE price_histories
    ALTER COLUMN currency_code TYPE VARCHAR(3);

ALTER TABLE retailers
    ALTER COLUMN country_code TYPE VARCHAR(2);
