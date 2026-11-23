package br.com.guilherme.eshop.ordering.api;

import java.math.BigDecimal;
import java.time.Instant;

import br.com.guilherme.eshop.ordering.repository.OrderSummary;

/** The API's OrderSummary: { id, createdAt, total, itemCount }. itemCount is the sum of the lines' quantities. */
public record OrderSummaryDto(Long id, Instant createdAt, BigDecimal total, long itemCount) {

    public static OrderSummaryDto from(OrderSummary summary) {
        return new OrderSummaryDto(summary.id(), summary.createdAt(), summary.total(), summary.itemCount());
    }
}
