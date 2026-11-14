package cl.udec.cocinavecina.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import java.time.OffsetDateTime;

@Entity
@Table(name = "orders")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotNull
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "request_id", nullable = false)
    private PurchaseRequest request;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "offer_id", nullable = false)
    private Offer offer;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "buyer_id", nullable = false)
    private AppUser buyer;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seller_id", nullable = false)
    private AppUser seller;

    @NotNull
    @Column(name = "agreed_price", nullable = false)
    private Integer agreedPrice;

    @NotNull
    @Column(name = "portions", nullable = false)
    private Integer portions;

    @Size(max = 20)
    @NotNull
    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @NotNull
    @Column(name = "accepted_at", nullable = false)
    private OffsetDateTime acceptedAt;

    @Column(name = "ready_at")
    private OffsetDateTime readyAt;

    @Column(name = "delivered_at")
    private OffsetDateTime deliveredAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cancelled_by")
    private AppUser cancelledBy;

    @Size(max = 200)
    @Column(name = "cancellation_reason", length = 200)
    private String cancellationReason;

    @Column(name = "cancelled_at")
    private OffsetDateTime cancelledAt;

    @NotNull
    @Column(name = "window_end", nullable = false)
    private OffsetDateTime windowEnd;

    @Size(max = 64)
    @Column(name = "code_hmac", length = 64)
    private String codeHmac;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "code_attempts", nullable = false)
    private Integer codeAttempts;

    @NotNull
    @ColumnDefault("0")
    @Column(name = "code_issues", nullable = false)
    private Integer codeIssues;


}
