package cl.udec.cocinavecina.service;

import cl.udec.cocinavecina.components.FieldEncryptor;
import cl.udec.cocinavecina.components.TokenIssuer;
import cl.udec.cocinavecina.components.VerificationMailer;
import cl.udec.cocinavecina.dto.Auth;
import cl.udec.cocinavecina.exception.ApiException;
import cl.udec.cocinavecina.model.AccountStatus;
import cl.udec.cocinavecina.model.AppUser;
import cl.udec.cocinavecina.model.Role;
import cl.udec.cocinavecina.model.VerificationToken;
import cl.udec.cocinavecina.repository.AppUserRepository;
import cl.udec.cocinavecina.repository.VerificationTokenRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Service
public class AuthService {
    private static final Duration LINK_TTL = Duration.ofHours(24);

    private final AppUserRepository users;
    private final VerificationTokenRepository verificationTokens;
    private final ConsentService consentService;
    private final RefreshTokenService refreshTokens;
    private final PasswordEncoder encoder;
    private final FieldEncryptor encryptor;
    private final TokenIssuer tokenIssuer;
    private final VerificationMailer mailer;
    private final AuditService audit;
    private final SecureRandom random = new SecureRandom();

    public AuthService(
        AppUserRepository users,
        VerificationTokenRepository verificationTokens,
        ConsentService consentService,
        RefreshTokenService refreshTokens,
        PasswordEncoder encoder,
        FieldEncryptor encryptor,
        TokenIssuer tokenIssuer,
        VerificationMailer mailer,
        AuditService audit
    ) {
        this.users = users;
        this.verificationTokens = verificationTokens;
        this.consentService = consentService;
        this.refreshTokens = refreshTokens;
        this.encoder = encoder;
        this.encryptor = encryptor;
        this.tokenIssuer = tokenIssuer;
        this.mailer = mailer;
        this.audit = audit;
    }

    @Transactional
    public Auth.SignUpResponse register(Auth.SignUpRequest request) {
        if (request.role() == Role.MODERATOR) {
            throw ApiException.forbidden("El rol moderador no se obtiene por registro");
        }
        final String phone = request.phone() == null || request.phone().isBlank()
            ? null
            : request.phone().replaceAll("\\s", "");
        if (request.role() == Role.SELLER && phone == null) {
            throw ApiException.invalid("phone: El teléfono es obligatorio para vender.");
        }
        final String email = request.email().trim().toLowerCase();
        final String displayName = request.displayName().trim();
        final List<String> conflicts = new ArrayList<>();
        if (users.existsByEmail(email)) {
            conflicts.add("email: Ya existe una cuenta con el correo electrónico.");
        }
        if (users.existsByDisplayNameIgnoreCase(displayName)) {
            conflicts.add("displayName: Ya existe una cuenta con ese nombre de usuario.");
        }
        if (!conflicts.isEmpty()) {
            throw ApiException.conflict(conflicts.toArray(String[]::new));
        }

        final OffsetDateTime now = OffsetDateTime.now();
        final AppUser user = users.save(AppUser.builder()
            .email(email)
            .displayName(displayName)
            .passwordHash(encoder.encode(request.password()))
            .encryptedPhone(encryptor.encrypt(phone))
            .role(request.role())
            .adultDeclared(request.adult())
            .status(AccountStatus.PENDING_VERIFICATION)
            .createdAt(now)
            .build());
        consentService.grantAtSignUp(user, request.acceptsLocation(), request.acceptsNotifications(), now);
        final VerificationToken token = verificationTokens.save(VerificationToken.builder()
            .user(user)
            .token(randomToken())
            .expiresAt(now.plus(LINK_TTL))
            .build());

        mailer.send(user.getEmail(), token.getToken());
        audit.log(user.getId(), "SIGN_UP", "user", user.getId());
        return new Auth.SignUpResponse(user.getId(), "Revisa tu correo para verificar la cuenta");
    }

    @Transactional
    public void verify(String tokenValue) {
        final OffsetDateTime now = OffsetDateTime.now();
        final VerificationToken token = verificationTokens.findByToken(tokenValue)
            .filter(t -> t.getUsedAt() == null && t.getExpiresAt().isAfter(now))
            .orElseThrow(() -> ApiException.conflict("El enlace no es válido o ya venció"));
        token.setUsedAt(now);
        final AppUser user = token.getUser();
        if (user.getStatus() == AccountStatus.PENDING_VERIFICATION) {
            user.setStatus(AccountStatus.ACTIVE);
            user.setVerifiedAt(now);
        }
    }

    @Transactional
    public Auth.TokenResponse login(Auth.LoginRequest request) {
        final AppUser user = users.findByEmail(request.email().trim().toLowerCase())
            .filter(u -> encoder.matches(request.password(), u.getPasswordHash()))
            .orElseThrow(() -> ApiException.unauthorized("Correo o contraseña incorrectos"));
        if (user.getStatus() == AccountStatus.SUSPENDED || user.getStatus() == AccountStatus.DELETED) {
            throw ApiException.forbidden("La cuenta no está activa");
        }
        audit.log(user.getId(), "LOGIN", "user", user.getId());
        return tokenIssuer.issue(user, refreshTokens.start(user));
    }

    public Auth.TokenResponse refresh(String refreshToken) {
        return refreshTokens.rotate(refreshToken);
    }

    public void logout(String refreshToken) {
        refreshTokens.logout(refreshToken);
    }

    private String randomToken() {
        final byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
