package cl.udec.cocinavecina.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "purchase_request")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class PurchaseRequest {
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
    @JoinColumn(name = "buyer_id", nullable = false)
    private AppUser buyer;

    @NotNull
    @Column(name = "portions", nullable = false)
    private Integer portions;

    @NotNull
    @Column(name = "rounds", nullable = false)
    private Integer rounds;

    @NotNull
    @Column(name = "current_price", nullable = false)
    private Integer currentPrice;

    @Size(max = 20)
    @NotNull
    @Column(name = "current_party", nullable = false, length = 20)
    private String currentParty;

    @Size(max = 20)
    @NotNull
    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Size(max = 100)
    @Column(name = "close_reason", length = 100)
    private String closeReason;

    @NotNull
    @Column(name = "offer_expires_at", nullable = false)
    private OffsetDateTime offerExpiresAt;

    @NotNull
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @NotNull
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;


}
