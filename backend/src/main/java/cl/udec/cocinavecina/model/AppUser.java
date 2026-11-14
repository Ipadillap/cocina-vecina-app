package cl.udec.cocinavecina.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "app_user")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class AppUser {
    @NotNull
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
    @Size(max = 30)
    @NotNull
    @Column(name = "status", nullable = false, length = 30)
    private String status;
    @Column(name = "verified_at")
    private OffsetDateTime verifiedAt;
    @NotNull
    @Column(name = "adult_declared", nullable = false)
    private Boolean adultDeclared;
    @Size(max = 20)
    @NotNull
    @Column(name = "role", nullable = false, length = 20)
    private String role;
    @Column(name = "encrypted_phone", length = Integer.MAX_VALUE)
    private String encryptedPhone;
    @Size(max = 255)
    @NotNull
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;
    @Size(max = 60)
    @NotNull
    @Column(name = "display_name", nullable = false, length = 60)
    private String displayName;
    @Size(max = 254)
    @NotNull
    @Column(name = "email", nullable = false, length = 254)
    private String email;
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

}
