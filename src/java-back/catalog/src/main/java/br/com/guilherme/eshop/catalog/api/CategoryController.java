package br.com.guilherme.eshop.catalog.api;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.guilherme.eshop.catalog.repository.ProductRepository;

@RestController
public class CategoryController {

    private final ProductRepository productRepository;

    public CategoryController(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    /** Distinct product categories, sorted, for the storefront's category filter. */
    @GetMapping("/categories")
    public List<String> list() {
        return productRepository.findDistinctCategories();
    }
}
