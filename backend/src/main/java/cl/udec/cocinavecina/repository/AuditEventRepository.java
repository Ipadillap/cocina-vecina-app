package cl.udec.cocinavecina.repository;

import cl.udec.cocinavecina.model.AuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;

public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {
    @Modifying
    @Query("delete from AuditEvent a where a.occurredAt < :cutoff")
    int deleteOlderThan(@Param("cutoff") OffsetDateTime cutoff);
}
