package br.com.guilherme.eshop.catalog.api;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import br.com.guilherme.eshop.catalog.domain.Product;
import br.com.guilherme.eshop.catalog.repository.ProductRepository;
import br.com.guilherme.eshop.catalog.repository.ProductSpecifications;

@RestController
public class ProductController {

    static final int MAX_PAGE_SIZE = 100;

    private final ProductRepository productRepository;

    public ProductController(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    /** Paged product list, optionally filtered by exact category and case-insensitive name search, ordered by name. */
    @GetMapping("/products")
    public PageDto<ProductDto> list(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size) {
        if (page < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "page must be >= 0");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "size must be between 1 and " + MAX_PAGE_SIZE);
        }

        List<Specification<Product>> filters = new ArrayList<>();
        if (category != null && !category.isBlank()) {
            filters.add(ProductSpecifications.hasCategory(category.trim()));
        }
        if (q != null && !q.isBlank()) {
            filters.add(ProductSpecifications.nameContainsIgnoreCase(q.trim()));
        }
        Specification<Product> spec = filters.isEmpty() ? Specification.unrestricted() : Specification.allOf(filters);

        // id breaks ties between equal names so paging is stable.
        PageRequest pageable = PageRequest.of(page, size, Sort.by("name").and(Sort.by("id")));
        return PageDto.from(productRepository.findAll(spec, pageable).map(ProductDto::from));
    }
}
