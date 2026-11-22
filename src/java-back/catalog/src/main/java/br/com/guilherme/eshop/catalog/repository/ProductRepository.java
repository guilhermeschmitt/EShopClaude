package br.com.guilherme.eshop.catalog.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.guilherme.eshop.catalog.domain.Product;

public interface ProductRepository extends JpaRepository<Product, Long> {
}
