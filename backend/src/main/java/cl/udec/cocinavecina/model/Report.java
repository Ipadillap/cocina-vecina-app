package cl.udec.cocinavecina.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import java.time.OffsetDateTime;

@Entity
@Table(name = "report")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class Report {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "offer_id", nullable = false)
    private Offer offer;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seller_id", nullable = false)
    private AppUser seller;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reporter_id", nullable = false)
    private AppUser reporter;

    @Size(max = 30)
    @NotNull
    @Column(name = "reason", nullable = false, length = 30)
    private String reason;

    @Size(max = 300)
    @Column(name = "detail", length = 300)
    private String detail;

    @NotNull
    @ColumnDefault("false")
    @Column(name = "resolved", nullable = false)
    private Boolean resolved;

    @NotNull
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;


}
