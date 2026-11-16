package cl.udec.cocinavecina.repository;

import cl.udec.cocinavecina.model.Order;
import cl.udec.cocinavecina.projection.OrderSummaryProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;

import java.util.Collection;
import java.util.Optional;
import java.util.List;

public interface OrdersRepository extends JpaRepository<Order, Long> {
    @Query(
        value = """
            select o.id, o.offer_id, f.title as offer_title,
                   case when o.buyer_id = :userId then 'BUYER' else 'SELLER' end as side,
                   o.agreed_price, o.portions, o.status, o.accepted_at
            from orders o
            join offer f on f.id = o.offer_id
            where o.buyer_id = :userId or o.seller_id = :userId
            order by o.accepted_at desc
            """,
        nativeQuery = true
    )
    List<OrderSummaryProjection> findSummariesByUser(@Param("userId") Long userId);

    @Query(
        value = """
            select exists (
                select 1
                from orders o
                where (o.buyer_id = :userId or o.seller_id = :userId)
                  and o.status in ('ACCEPTED', 'READY_FOR_PICKUP')
            )
            """,
        nativeQuery = true
    )
    boolean hasActiveOrders(@Param("userId") Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from Order o where o.id = :id")
    Optional<Order> lockById(@Param("id") Long id);

    List<Order> findByBuyer_IdOrSeller_IdOrderByAcceptedAtDesc(Long buyerId, Long sellerId);

    List<Order> findByRequest_IdIn(Collection<Long> requestIds);
}
