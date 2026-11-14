package cl.udec.cocinavecina.repository;

import cl.udec.cocinavecina.model.AuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {

}
