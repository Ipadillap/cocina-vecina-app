package cl.udec.cocinavecina.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "device")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class Device {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Size(max = 512)
    @NotNull
    @Column(name = "push_token", nullable = false, length = 512)
    private String pushToken;

    @Size(max = 20)
    @NotNull
    @Column(name = "platform", nullable = false, length = 20)
    private String platform;

    @NotNull
    @Column(name = "registered_at", nullable = false)
    private OffsetDateTime registeredAt;


}
