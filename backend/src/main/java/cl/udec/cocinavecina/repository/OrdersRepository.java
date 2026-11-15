package cl.udec.cocinavecina.repository;

import cl.udec.cocinavecina.model.Order;
import cl.udec.cocinavecina.projection.OrderSummaryProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrdersRepository extends JpaRepository<Order, Long> {

    @Query(value = """
        SELECT o.id, o.offer_id, f.title AS offer_title,
               CASE WHEN o.buyer_id = :userId THEN 'BUYER' ELSE 'SELLER' END AS side,
               o.agreed_price, o.portions, o.status, o.accepted_at
        FROM orders o
        JOIN offer f ON f.id = o.offer_id
        WHERE o.buyer_id = :userId OR o.seller_id = :userId
        ORDER BY o.accepted_at DESC
        """, nativeQuery = true)
    List<OrderSummaryProjection> findSummariesByUser(@Param("userId") Long userId);

    @Query(value = """
        SELECT EXISTS (
            SELECT 1
            FROM orders o
            WHERE (o.buyer_id = :userId OR o.seller_id = :userId)
              AND o.status IN ('ACCEPTED', 'READY_FOR_PICKUP')
        )
        """, nativeQuery = true)
    boolean hasActiveOrders(@Param("userId") Long userId);
}
