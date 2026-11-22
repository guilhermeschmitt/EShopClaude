package br.com.guilherme.eshop.catalog.repository;

import java.util.Locale;

import org.springframework.data.jpa.domain.Specification;

import br.com.guilherme.eshop.catalog.domain.Product;

/** Optional product filters, combined by the caller only when the filter is present. */
public final class ProductSpecifications {

    private static final char LIKE_ESCAPE = '\\';

    private ProductSpecifications() {
    }

    /** Exact, case-sensitive match on the category. */
    public static Specification<Product> hasCategory(String category) {
        return (root, query, cb) -> cb.equal(root.get("category"), category);
    }

    /** Case-insensitive "contains" match on the name; LIKE wildcards in {@code text} are matched literally. */
    public static Specification<Product> nameContainsIgnoreCase(String text) {
        String pattern = "%" + escapeLike(text.toLowerCase(Locale.ROOT)) + "%";
        return (root, query, cb) -> cb.like(cb.lower(root.get("name")), pattern, LIKE_ESCAPE);
    }

    private static String escapeLike(String text) {
        return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
