package cl.udec.cocinavecina.repository;

import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.QueryHints;
import java.time.OffsetDateTime;
import cl.udec.cocinavecina.model.OfferStatus;
import cl.udec.cocinavecina.model.PurchaseRequest;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PurchaseRequestRepository extends JpaRepository<PurchaseRequest, Long> {
    @Modifying(flushAutomatically = true)
    @Query(
        value = """
            update purchase_request
            set status = 'EXPIRED', close_reason = :reason, updated_at = now()
            where offer_id = :offerId and status in ('OFFER_SENT', 'COUNTEROFFER')
            """,
        nativeQuery = true
    )
    void expireOpenByOffer(@Param("offerId") Long offerId, @Param("reason") String reason);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from PurchaseRequest r where r.id = :id")
    Optional<PurchaseRequest> lockById(@Param("id") Long id);

    boolean existsByOffer_IdAndBuyer_IdAndStatusIn(Long offerId, Long buyerId, Collection<String> statuses);

    List<PurchaseRequest> findBySeller_IdAndStatusOrderByCreatedAtAsc(Long sellerId, String status);

    List<PurchaseRequest> findByBuyer_IdOrderByUpdatedAtDesc(Long buyerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
    @Query("""
        select r from PurchaseRequest r
        where r.status in :open
          and (r.offerExpiresAt <= :now
               or r.offer.id in (select o.id from Offer o where o.status in :dead))
        """)
    List<PurchaseRequest> findExpirable(
        @Param("open") Collection<String> open,
        @Param("dead") Collection<OfferStatus> dead,
        @Param("now") OffsetDateTime now
    );

    @Modifying(flushAutomatically = true)
    @Query(
        value = """
            update purchase_request
            set status = 'CANCELLED', close_reason = :reason, updated_at = now()
            where buyer_id = :buyerId and status in ('OFFER_SENT', 'COUNTEROFFER')
            """,
        nativeQuery = true
    )
    void cancelOpenByBuyer(@Param("buyerId") Long buyerId, @Param("reason") String reason);
}
