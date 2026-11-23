package br.com.guilherme.eshop.ordering.repository;

import java.math.BigDecimal;
import java.time.Instant;

/** One order of a buyer, without its lines. {@code itemCount} is the sum of the lines' quantities. */
public record OrderSummary(Long id, Instant createdAt, BigDecimal total, Long itemCount) {
}
