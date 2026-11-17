package cl.udec.cocinavecina.service;

import cl.udec.cocinavecina.components.TokenIssuer;
import cl.udec.cocinavecina.dto.Auth;
import cl.udec.cocinavecina.exception.ApiException;
import cl.udec.cocinavecina.model.AccountStatus;
import cl.udec.cocinavecina.model.AppUser;
import cl.udec.cocinavecina.model.RefreshToken;
import cl.udec.cocinavecina.repository.RefreshTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class RefreshTokenService {
    private static final String INVALID = "La sesión expiró; vuelve a iniciar sesión";

    private final RefreshTokenRepository tokens;
    private final TokenIssuer issuer;
    private final AuditService audit;
    private final Duration ttl;
    private final SecureRandom random = new SecureRandom();

    public RefreshTokenService(
        RefreshTokenRepository tokens,
        TokenIssuer issuer,
        AuditService audit,
        @Value("${cocinavecina.jwt.refresh-token-ttl:30d}") Duration ttl
    ) {
        this.tokens = tokens;
        this.issuer = issuer;
        this.audit = audit;
        this.ttl = ttl;
    }

    @Transactional
    public String start(AppUser user) {
        return create(user, UUID.randomUUID().toString(), OffsetDateTime.now());
    }

    @Transactional(noRollbackFor = ApiException.class)
    public Auth.TokenResponse rotate(String raw) {
        final OffsetDateTime now = OffsetDateTime.now();
        final RefreshToken current = tokens.lockByTokenHash(hash(raw))
            .orElseThrow(() -> ApiException.unauthorized(INVALID));
        final AppUser user = current.getUser();
        if (current.getRevokedAt() != null) {
            tokens.revokeFamily(current.getFamilyId(), now);
            audit.log(user.getId(), "REFRESH_TOKEN_REUSED", "user", user.getId());
            throw ApiException.unauthorized(INVALID);
        }
        if (!current.getExpiresAt().isAfter(now)) {
            throw ApiException.unauthorized(INVALID);
        }
        if (user.getStatus() == AccountStatus.SUSPENDED || user.getStatus() == AccountStatus.DELETED) {
            tokens.revokeFamily(current.getFamilyId(), now);
            throw ApiException.forbidden("La cuenta no está activa");
        }
        current.setRevokedAt(now);
        final String next = create(user, current.getFamilyId(), now);
        audit.log(user.getId(), "TOKEN_REFRESHED", "user", user.getId());
        return issuer.issue(user, next);
    }

    @Transactional
    public void logout(String raw) {
        tokens.findByTokenHash(hash(raw)).ifPresent(t -> {
            tokens.revokeFamily(t.getFamilyId(), OffsetDateTime.now());
            audit.log(t.getUser().getId(), "LOGOUT", "user", t.getUser().getId());
        });
    }

    @Transactional
    public void revokeAll(Long userId) {
        tokens.revokeAllByUser(userId, OffsetDateTime.now());
    }

    @Transactional
    public int purgeExpired() {
        return tokens.deleteExpired(OffsetDateTime.now());
    }

    private String create(AppUser user, String familyId, OffsetDateTime now) {
        final byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        final String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        tokens.save(RefreshToken.builder()
            .user(user)
            .familyId(familyId)
            .tokenHash(hash(raw))
            .createdAt(now)
            .expiresAt(now.plus(ttl))
            .build());
        return raw;
    }

    private static String hash(String raw) {
        try {
            return HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8))
            );
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}
