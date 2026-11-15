package cl.udec.cocinavecina.service;

import cl.udec.cocinavecina.model.AuditEvent;
import cl.udec.cocinavecina.repository.AuditEventRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
public class AuditService {
    private final AuditEventRepository auditEvents;

    public AuditService(AuditEventRepository auditEvents) {
        this.auditEvents = auditEvents;
    }

    @Transactional
    public void log(Long actorId, String action, String entity, Long entityId) {
        auditEvents.save(AuditEvent.builder()
            .actorId(actorId)
            .action(action)
            .entity(entity)
            .entityId(entityId)
            .occurredAt(OffsetDateTime.now())
            .build());
    }
}
