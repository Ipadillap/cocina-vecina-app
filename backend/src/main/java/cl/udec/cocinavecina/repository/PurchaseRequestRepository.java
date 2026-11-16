package cl.udec.cocinavecina.repository;

import cl.udec.cocinavecina.model.PurchaseRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
}
