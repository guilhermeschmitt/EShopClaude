package br.com.guilherme.eshop.ordering.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.guilherme.eshop.ordering.domain.Order;

public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * A buyer's orders as summaries, newest first (ties on createdAt broken by id, newest first).
     * One query; the order lines are aggregated, never loaded. Every order has at least one line,
     * so an inner join is enough.
     */
    @Query("""
            select new br.com.guilherme.eshop.ordering.repository.OrderSummary(o.id, o.createdAt, o.total, sum(i.quantity))
            from Order o join o.items i
            where o.buyerId = :buyerId
            group by o.id, o.createdAt, o.total
            order by o.createdAt desc, o.id desc
            """)
    List<OrderSummary> findSummariesByBuyerId(@Param("buyerId") UUID buyerId);
}
