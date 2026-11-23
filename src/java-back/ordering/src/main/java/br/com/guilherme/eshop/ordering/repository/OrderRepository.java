package br.com.guilherme.eshop.ordering.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.guilherme.eshop.ordering.domain.Order;

public interface OrderRepository extends JpaRepository<Order, Long> {
}
