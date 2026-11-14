package cl.udec.cocinavecina.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "audit_event")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class AuditEvent {
    @NotNull
    @Column(name = "occurred_at", nullable = false)
    private OffsetDateTime occurredAt;
    @Size(max = 300)
    @Column(name = "detail", length = 300)
    private String detail;
    @Column(name = "entity_id")
    private Long entityId;
    @Size(max = 30)
    @NotNull
    @Column(name = "entity", nullable = false, length = 30)
    private String entity;
    @Size(max = 60)
    @NotNull
    @Column(name = "action", nullable = false, length = 60)
    private String action;
    @Column(name = "actor_id")
    private Long actorId;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;
}
