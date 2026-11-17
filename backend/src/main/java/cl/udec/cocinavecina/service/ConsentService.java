package cl.udec.cocinavecina.service;

import cl.udec.cocinavecina.dto.ConsentView;
import cl.udec.cocinavecina.exception.ApiException;
import cl.udec.cocinavecina.model.AccountStatus;
import cl.udec.cocinavecina.model.AppUser;
import cl.udec.cocinavecina.model.Consent;
import cl.udec.cocinavecina.model.ConsentType;
import cl.udec.cocinavecina.repository.AppUserRepository;
import cl.udec.cocinavecina.repository.ConsentRespository;
import cl.udec.cocinavecina.repository.DeviceRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

@Service
public class ConsentService {
    private final ConsentRespository consents;
    private final AppUserRepository users;
    private final DeviceRepository devices;
    private final AuditService audit;
    private final String textVersion;

    public ConsentService(
        ConsentRespository consents,
        AppUserRepository users,
        DeviceRepository devices,
        AuditService audit,
        @Value("${cocinavecina.consent.text-version:v1}") String textVersion
    ) {
        this.consents = consents;
        this.users = users;
        this.devices = devices;
        this.audit = audit;
        this.textVersion = textVersion;
    }

    @Transactional
    public void grantAtSignUp(AppUser user, boolean location, boolean notifications, OffsetDateTime now) {
        grant(user, ConsentType.TERMS, now);
        if (location) {
            grant(user, ConsentType.LOCATION, now);
        }
        if (notifications) {
            grant(user, ConsentType.NOTIFICATIONS, now);
        }
    }

    @Transactional(readOnly = true)
    public List<ConsentView> list(Long userId) {
        final List<Consent> all = consents.findByUser_IdOrderByGrantedAtAscIdAsc(userId);
        return Arrays.stream(ConsentType.values())
            .map(type -> all.stream()
                .filter(c -> c.getType().equals(type.name()))
                .max(Comparator.comparing(Consent::getGrantedAt).thenComparing(Consent::getId))
                .map(ConsentService::toView)
                .orElse(new ConsentView(type, false, null, null, null)))
            .toList();
    }

    @Transactional
    public ConsentView set(Long userId, ConsentType type, boolean granted) {
        final AppUser user = users.findById(userId)
            .orElseThrow(() -> ApiException.notFound("No existe el usuario " + userId));
        if (user.getStatus() == AccountStatus.DELETED) {
            throw ApiException.forbidden("La cuenta no está activa");
        }
        final OffsetDateTime now = OffsetDateTime.now();
        final Consent active = consents
            .findFirstByUser_IdAndTypeAndRevokedAtIsNullOrderByGrantedAtDesc(userId, type.name())
            .orElse(null);
        if (granted) {
            if (active != null) {
                return toView(active);
            }
            final Consent created = grant(user, type, now);
            audit.log(userId, "CONSENT_GRANTED", "consent", created.getId(), type.name());
            return toView(created);
        }
        if (type == ConsentType.TERMS) {
            throw ApiException.rule("RB-9", "Los términos son obligatorios; para retirarlos elimina tu cuenta");
        }
        if (active == null) {
            return list(userId).stream().filter(v -> v.type() == type).findFirst().orElseThrow();
        }
        active.setRevokedAt(now);
        if (type == ConsentType.NOTIFICATIONS) {
            devices.deleteByUser_Id(userId);
        }
        audit.log(userId, "CONSENT_REVOKED", "consent", active.getId(), type.name());
        return toView(active);
    }

    private Consent grant(AppUser user, ConsentType type, OffsetDateTime now) {
        return consents.save(Consent.builder()
            .user(user)
            .type(type.name())
            .grantedAt(now)
            .textVersion(textVersion)
            .build());
    }

    private static ConsentView toView(Consent c) {
        return new ConsentView(
            ConsentType.valueOf(c.getType()),
            c.getRevokedAt() == null,
            c.getGrantedAt().toInstant(),
            c.getRevokedAt() == null ? null : c.getRevokedAt().toInstant(),
            c.getTextVersion()
        );
    }
}
