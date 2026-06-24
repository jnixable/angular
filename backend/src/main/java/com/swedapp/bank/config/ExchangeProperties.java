package com.swedapp.bank.config;

import java.math.BigDecimal;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.swedapp.bank.domain.Currency;

@ConfigurationProperties(prefix = "app.exchange")
public record ExchangeProperties(Currency baseCurrency, Map<Currency, BigDecimal> rates) {
}
