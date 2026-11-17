package cl.udec.cocinavecina.repository;

import cl.udec.cocinavecina.model.RefreshToken;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from RefreshToken t where t.tokenHash = :hash")
    Optional<RefreshToken> lockByTokenHash(@Param("hash") String hash);

    @Modifying(flushAutomatically = true)
    @Query(
        value = "update refresh_token set revoked_at = :now where family_id = :familyId and revoked_at is null",
        nativeQuery = true
    )
    void revokeFamily(@Param("familyId") String familyId, @Param("now") OffsetDateTime now);

    @Modifying(flushAutomatically = true)
    @Query(
        value = "update refresh_token set revoked_at = :now where user_id = :userId and revoked_at is null",
        nativeQuery = true
    )
    void revokeAllByUser(@Param("userId") Long userId, @Param("now") OffsetDateTime now);

    @Modifying
    @Query("delete from RefreshToken t where t.expiresAt < :now")
    int deleteExpired(@Param("now") OffsetDateTime now);
}
