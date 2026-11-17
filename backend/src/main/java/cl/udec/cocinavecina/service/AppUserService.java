package cl.udec.cocinavecina.service;

import cl.udec.cocinavecina.components.FieldEncryptor;
import cl.udec.cocinavecina.dto.AccountExport;
import cl.udec.cocinavecina.dto.AccountView;
import cl.udec.cocinavecina.exception.ApiException;
import cl.udec.cocinavecina.model.AccountStatus;
import cl.udec.cocinavecina.model.AppUser;
import cl.udec.cocinavecina.model.Consent;
import cl.udec.cocinavecina.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class AppUserService {
    private final AppUserRepository users;
    private final OfferRepository offers;
    private final OrdersRepository orders;
    private final ConsentRespository consents;
    private final PurchaseRequestRepository purchaseRequests;
    private final DeviceRepository devices;
    private final OfferService offerService;
    private final ConsentService consentService;
    private final RatingService ratingService;
    private final RefreshTokenService refreshTokens;
    private final PhotoService photos;
    private final FieldEncryptor encryptor;
    private final AuditService audit;

    public AppUserService(
        AppUserRepository users,
        OfferRepository offers,
        OrdersRepository orders,
        ConsentRespository consents,
        PurchaseRequestRepository purchaseRequests,
        DeviceRepository devices,
        OfferService offerService,
        ConsentService consentService,
        RatingService ratingService,
        RefreshTokenService refreshTokens,
        PhotoService photos,
        FieldEncryptor encryptor,
        AuditService audit
    ) {
        this.users = users;
        this.offers = offers;
        this.orders = orders;
        this.consents = consents;
        this.purchaseRequests = purchaseRequests;
        this.devices = devices;
        this.offerService = offerService;
        this.consentService = consentService;
        this.ratingService = ratingService;
        this.refreshTokens = refreshTokens;
        this.photos = photos;
        this.encryptor = encryptor;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public AccountView account(Long userId) {
        final AppUser user = get(userId);
        final Consent terms = consents.findFirstByUser_IdAndTypeAndRevokedAtIsNullOrderByGrantedAtDesc(userId, "TERMS")
            .orElse(null);
        return new AccountView(
            user.getId(),
            user.getEmail(),
            user.getDisplayName(),
            encryptor.decrypt(user.getEncryptedPhone()),
            user.getRole(),
            user.getVerifiedAt() != null,
            terms == null ? null : terms.getTextVersion(),
            terms == null ? null : terms.getGrantedAt().toInstant()
        );
    }

    @Transactional(readOnly = true)
    public AccountExport export(Long userId) {
        final AppUser user = get(userId);
        final AccountExport.Account account = new AccountExport.Account(
            user.getId(),
            user.getEmail(),
            user.getDisplayName(),
            encryptor.decrypt(user.getEncryptedPhone()),
            user.getRole(),
            user.getStatus(),
            Boolean.TRUE.equals(user.getAdultDeclared()),
            user.getCreatedAt().toInstant(),
            user.getVerifiedAt() == null ? null : user.getVerifiedAt().toInstant()
        );
        final List<AccountExport.OrderSummary> orderSummaries = orders.findSummariesByUser(userId).stream()
            .map(o -> new AccountExport.OrderSummary(
                o.getId(),
                o.getOfferId(),
                o.getOfferTitle(),
                o.getSide(),
                o.getAgreedPrice(),
                o.getPortions(),
                o.getStatus(),
                o.getAcceptedAt()
            ))
            .toList();
        return new AccountExport(
            account,
            offerService.bySeller(userId),
            orderSummaries,
            consentService.list(userId),
            ratingService.entriesOf(userId)
        );
    }

    @Transactional
    public boolean delete(Long userId) {
        final AppUser user = get(userId);
        if (user.getStatus() == AccountStatus.DELETED) {
            return true;
        }
        if (orders.hasActiveOrders(userId)) {
            if (user.getDeletionRequestedAt() == null) {
                user.setDeletionRequestedAt(OffsetDateTime.now());
                audit.log(userId, "ACCOUNT_DELETION_REQUESTED", "user", userId);
            }
            if (user.getStatus() == AccountStatus.ACTIVE) {
                user.setStatus(AccountStatus.PENDING_DELETION);
            }
            offers.closeAllBySeller(userId);
            purchaseRequests.cancelOpenByBuyer(userId, "Cuenta en eliminación");
            return false;
        }
        anonymize(user);
        return true;
    }

    @Transactional
    public int finalizePendingDeletions() {
        final List<AppUser> pending = users.findPendingDeletion(AccountStatus.DELETED);
        pending.forEach(this::anonymize);
        return pending.size();
    }

    private void anonymize(AppUser user) {
        final Long userId = user.getId();
        offers.findPhotoKeysBySeller(userId).forEach(photos::delete);
        offers.clearPhotosBySeller(userId);
        offers.closeAllBySeller(userId);
        purchaseRequests.cancelOpenByBuyer(userId, "Cuenta eliminada");
        devices.deleteByUser_Id(userId);
        refreshTokens.revokeAll(userId);
        user.setEmail("deleted-" + userId + "@cocinavecina.invalid");
        user.setDisplayName("Usuario eliminado " + userId);
        user.setPasswordHash("!");
        user.setEncryptedPhone(null);
        user.setStatus(AccountStatus.DELETED);
        audit.log(userId, "ACCOUNT_DELETED", "user", userId);
    }

    private AppUser get(Long userId) {
        return users.findById(userId)
            .orElseThrow(() -> ApiException.notFound("No existe el usuario " + userId));
    }
}
