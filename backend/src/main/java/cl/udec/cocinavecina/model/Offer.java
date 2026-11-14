package cl.udec.cocinavecina.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "offer")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class Offer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seller_id", nullable = false)
    private AppUser seller;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pickup_point_id", nullable = false)
    private PickupPoint pickupPoint;

    @Size(max = 80)
    @NotNull
    @Column(name = "title", nullable = false, length = 80)
    private String title;

    @Size(max = 300)
    @Column(name = "description", length = 300)
    private String description;

    @Size(max = 30)
    @NotNull
    @Column(name = "category", nullable = false, length = 30)
    private String category;

    @Size(max = 200)
    @Column(name = "photo_key", length = 200)
    private String photoKey;

    @NotNull
    @Column(name = "base_price", nullable = false)
    private Integer basePrice;

    @NotNull
    @Column(name = "min_price", nullable = false)
    private Integer minPrice;

    @NotNull
    @Column(name = "total_portions", nullable = false)
    private Integer totalPortions;

    @NotNull
    @Column(name = "available_portions", nullable = false)
    private Integer availablePortions;

    @NotNull
    @Column(name = "prepared_at", nullable = false)
    private OffsetDateTime preparedAt;

    @NotNull
    @Column(name = "published_at", nullable = false)
    private OffsetDateTime publishedAt;

    @NotNull
    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "closed_at")
    private OffsetDateTime closedAt;

    @Size(max = 20)
    @NotNull
    @Column(name = "status", nullable = false, length = 20)
    private String status;


}
