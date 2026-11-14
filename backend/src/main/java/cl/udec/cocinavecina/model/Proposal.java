package cl.udec.cocinavecina.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "proposal")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class Proposal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "request_id", nullable = false)
    private PurchaseRequest request;

    @Size(max = 20)
    @NotNull
    @Column(name = "party", nullable = false, length = 20)
    private String party;

    @NotNull
    @Column(name = "unit_price", nullable = false)
    private Integer unitPrice;

    @NotNull
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;


}
