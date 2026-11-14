package cl.udec.cocinavecina.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.ColumnDefault;

import java.time.OffsetDateTime;

@Entity
@Table(name = "pickup_point")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class PickupPoint {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seller_id", nullable = false)
    private AppUser seller;

    @Size(max = 40)
    @NotNull
    @Column(name = "alias", nullable = false, length = 40)
    private String alias;

    @NotNull
    @Column(name = "encrypted_address", nullable = false, length = Integer.MAX_VALUE)
    private String encryptedAddress;

    @Column(name = "encrypted_instructions", length = Integer.MAX_VALUE)
    private String encryptedInstructions;

    @NotNull
    @Column(name = "encrypted_lat", nullable = false, length = Integer.MAX_VALUE)
    private String encryptedLat;

    @NotNull
    @Column(name = "encrypted_lon", nullable = false, length = Integer.MAX_VALUE)
    private String encryptedLon;

    @NotNull
    @Column(name = "cell_lat", nullable = false)
    private Double cellLat;

    @NotNull
    @Column(name = "cell_lon", nullable = false)
    private Double cellLon;

    @ColumnDefault("(st_setsrid(st_makepoint(cell_lon, cell_lat), 4326))")
    @Column(name = "cell_center", columnDefinition = "geography")
    private Object cellCenter;

    @NotNull
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;


}
