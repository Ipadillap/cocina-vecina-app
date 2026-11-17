package cl.udec.cocinavecina.service;

import cl.udec.cocinavecina.dto.*;
import cl.udec.cocinavecina.exception.ApiException;
import cl.udec.cocinavecina.model.*;
import cl.udec.cocinavecina.repository.AppUserRepository;
import cl.udec.cocinavecina.repository.OfferRepository;
import cl.udec.cocinavecina.repository.PurchaseRequestRepository;
import cl.udec.cocinavecina.repository.ReportRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class ModerationService {
    /**
     * RB-8: tres reportes de usuarios distintos ocultan la oferta.
     */
    private static final int HIDE_THRESHOLD = 3;

    private final ReportRepository reports;
    private final OfferRepository offers;
    private final AppUserRepository users;
    private final PurchaseRequestRepository purchaseRequests;
    private final AuditService audit;
    private final RefreshTokenService refreshTokens;

    public ModerationService(
        ReportRepository reports,
        OfferRepository offers,
        AppUserRepository users,
        PurchaseRequestRepository purchaseRequests,
        AuditService audit,
        RefreshTokenService refreshTokens
    ) {
        this.reports = reports;
        this.offers = offers;
        this.users = users;
        this.purchaseRequests = purchaseRequests;
        this.audit = audit;
        this.refreshTokens = refreshTokens;
    }

    @Transactional
    public void report(Long reporterId, Long offerId, NewReport request) {
        final AppUser reporter = users.findById(reporterId)
            .orElseThrow(() -> ApiException.notFound("No existe el usuario " + reporterId));
        if (reporter.getStatus() != AccountStatus.ACTIVE) {
            throw ApiException.forbidden("La cuenta debe estar verificada y activa");
        }
        final Offer offer = offers.lockById(offerId)
            .orElseThrow(() -> ApiException.notFound("No existe la oferta " + offerId));
        if (offer.getSeller().getId().equals(reporterId)) {
            throw ApiException.forbidden("No puede reportar su propia oferta");
        }
        if (reports.existsByOffer_IdAndReporter_Id(offerId, reporterId)) {
            return;
        }
        reports.save(Report.builder()
            .offer(offer)
            .seller(offer.getSeller())
            .reporter(reporter)
            .reason(request.reason().name())
            .detail(request.detail())
            .resolved(false)
            .createdAt(OffsetDateTime.now())
            .build());
        audit.log(reporterId, "OFFER_REPORTED", "offer", offerId, request.reason().name());
        final boolean visible = offer.getStatus() == OfferStatus.ACTIVE || offer.getStatus() == OfferStatus.SOLD_OUT;
        if (visible && reports.countByOffer_IdAndResolvedFalse(offerId) >= HIDE_THRESHOLD) {
            offer.setStatus(OfferStatus.HIDDEN);
            audit.log(null, "OFFER_HIDDEN", "offer", offerId);
        }
    }

    @Transactional(readOnly = true)
    public List<OfferUnderReview> hiddenOffers() {
        return offers.findByStatusOrderByPublishedAtAsc(OfferStatus.HIDDEN).stream()
            .map(o -> new OfferUnderReview(
                o.getId(),
                o.getTitle(),
                o.getSeller().getId(),
                reports.countBySeller_Id(o.getSeller().getId()),
                reports.findByOffer_IdAndResolvedFalseOrderByCreatedAtAsc(o.getId()).stream()
                    .map(r -> new ReportView(
                        ReportReason.valueOf(r.getReason()), r.getDetail(),
                        r.getCreatedAt().toInstant()
                    ))
                    .toList()
            ))
            .toList();
    }

    @Transactional
    public void decideOffer(Long moderatorId, Long offerId, OfferDecision decision) {
        final Offer offer = offers.lockById(offerId)
            .orElseThrow(() -> ApiException.notFound("No existe la oferta " + offerId));
        final OffsetDateTime now = OffsetDateTime.now();
        switch (decision.action()) {
            case REACTIVATE -> {
                if (offer.getStatus() != OfferStatus.HIDDEN) {
                    throw ApiException.conflict("Solo una oferta Oculta puede reactivarse");
                }
                if (!offer.getExpiresAt().isAfter(now)) {
                    offer.setStatus(OfferStatus.EXPIRED);
                } else if (offer.getAvailablePortions() == 0) {
                    offer.setStatus(OfferStatus.SOLD_OUT);
                } else {
                    offer.setStatus(OfferStatus.ACTIVE);
                }
                reports.resolveByOffer(offerId);
                audit.log(moderatorId, "OFFER_REACTIVATED", "offer", offerId, decision.reason());
            }
            case WITHDRAW -> {
                final OfferStatus status = offer.getStatus();
                if (status != OfferStatus.HIDDEN && status != OfferStatus.ACTIVE && status != OfferStatus.SOLD_OUT) {
                    throw ApiException.conflict("La oferta no puede retirarse en estado " + status);
                }
                offer.setStatus(OfferStatus.WITHDRAWN);
                offer.setClosedAt(now);
                reports.resolveByOffer(offerId);
                purchaseRequests.expireOpenByOffer(offerId, "OFFER_WITHDRAWN");
                audit.log(moderatorId, "OFFER_WITHDRAWN", "offer", offerId, decision.reason());
            }
        }
    }

    @Transactional
    public void decideAccount(Long moderatorId, Long userId, AccountDecision decision) {
        if (moderatorId.equals(userId)) {
            throw ApiException.forbidden("No puede moderar su propia cuenta");
        }
        final AppUser target = users.findById(userId)
            .orElseThrow(() -> ApiException.notFound("No existe el usuario " + userId));
        if (target.getRole() == Role.MODERATOR) {
            throw ApiException.forbidden("No se puede moderar la cuenta de otro moderador");
        }
        switch (decision.action()) {
            case SUSPEND -> {
                if (target.getStatus() != AccountStatus.ACTIVE) {
                    throw ApiException.conflict("Solo una cuenta Activa puede suspenderse");
                }
                target.setStatus(AccountStatus.SUSPENDED);
                offers.closeAllBySeller(userId);
                refreshTokens.revokeAll(userId);
                audit.log(moderatorId, "ACCOUNT_SUSPENDED", "app_user", userId, decision.reason());
            }
            case REACTIVATE -> {
                if (target.getStatus() != AccountStatus.SUSPENDED) {
                    throw ApiException.conflict("Solo una cuenta Suspendida puede reactivarse");
                }
                target.setStatus(AccountStatus.ACTIVE);
                audit.log(moderatorId, "ACCOUNT_REACTIVATED", "app_user", userId, decision.reason());
            }
        }
    }
}
