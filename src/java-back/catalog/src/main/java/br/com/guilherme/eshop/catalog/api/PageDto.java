package br.com.guilherme.eshop.catalog.api;

import java.util.List;

import org.springframework.data.domain.Page;

/** A page of results: { items, page, size, totalItems, totalPages }. The page number is zero-based. */
public record PageDto<T>(List<T> items, int page, int size, long totalItems, int totalPages) {

    public static <T> PageDto<T> from(Page<T> page) {
        return new PageDto<>(page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages());
    }
}
