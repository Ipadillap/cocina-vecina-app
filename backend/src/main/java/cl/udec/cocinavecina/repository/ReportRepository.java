package cl.udec.cocinavecina.repository;

import cl.udec.cocinavecina.model.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ReportRepository extends JpaRepository<Report, Long> {
    boolean existsByOffer_IdAndReporter_Id(Long offerId, Long reporterId);

    long countByOffer_IdAndResolvedFalse(Long offerId);

    long countBySeller_Id(Long sellerId);

    List<Report> findByOffer_IdAndResolvedFalseOrderByCreatedAtAsc(Long offerId);

    @Modifying(flushAutomatically = true)
    @Query("update Report r set r.resolved = true where r.offer.id = :offerId and r.resolved = false")
    void resolveByOffer(@Param("offerId") Long offerId);
}
