package br.com.guilherme.eshop.catalog.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import br.com.guilherme.eshop.catalog.domain.Product;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    /** Distinct product categories, sorted. */
    @Query("select distinct p.category from Product p order by p.category")
    List<String> findDistinctCategories();
}
