package cl.udec.cocinavecina.service;

import cl.udec.cocinavecina.components.RealtimeEvent;
import cl.udec.cocinavecina.dto.UserEvent;
import cl.udec.cocinavecina.model.*;
import cl.udec.cocinavecina.repository.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class MaintenanceService {
    private static final List<String> OPEN_REQUESTS = List.of(
        PurchaseRequestStatus.OFFER_SENT.name(),
        PurchaseRequestStatus.COUNTEROFFER.name()
    );
    private static final List<OfferStatus> DEAD_OFFERS = List.of(
        OfferStatus.EXPIRED, OfferStatus.CLOSED, OfferStatus.WITHDRAWN);
    private static final List<OfferStatus> ALIVE_OFFERS = List.of(
        OfferStatus.ACTIVE, OfferStatus.HIDDEN);
    private static final List<String> OPEN_ORDERS = List.of(
        OrderStatus.ACCEPTED.name(),
        OrderStatus.READY_FOR_PICKUP.name()
    );
    private static final int PHOTO_RETENTION_DAYS = 30;
    private static final int AUDIT_RETENTION_MONTHS = 12;
    private static final int VERIFICATION_TOKEN_RETENTION_DAYS = 7;

    private final OfferRepository offers;
    private final PurchaseRequestRepository requests;
    private final OrdersRepository orders;
    private final AuditEventRepository auditEvents;
    private final VerificationTokenRepository verificationTokens;
    private final RefreshTokenService refreshTokens;
    private final PhotoService photos;
    private final AuditService audit;
    private final ApplicationEventPublisher events;

    public MaintenanceService(
        OfferRepository offers,
        PurchaseRequestRepository requests,
        OrdersRepository orders,
        AuditEventRepository auditEvents,
        VerificationTokenRepository verificationTokens,
        RefreshTokenService refreshTokens,
        PhotoService photos,
        AuditService audit,
        ApplicationEventPublisher events
    ) {
        this.offers = offers;
        this.requests = requests;
        this.orders = orders;
        this.auditEvents = auditEvents;
        this.verificationTokens = verificationTokens;
        this.refreshTokens = refreshTokens;
        this.photos = photos;
        this.audit = audit;
        this.events = events;
    }

    @Transactional
    public int expireOffers() {
        return offers.expireDue(OffsetDateTime.now());
    }

    @Transactional
    public int expireRequests() {
        final OffsetDateTime now = OffsetDateTime.now();
        final List<PurchaseRequest> due = requests.findExpirable(OPEN_REQUESTS, DEAD_OFFERS, now);
        for (PurchaseRequest request : due) {
            final String reason = request.getOfferExpiresAt().isAfter(now) ? "Oferta cerrada" : "Oferta vencida";
            request.setStatus(PurchaseRequestStatus.EXPIRED.name());
            request.setCloseReason(reason);
            request.setUpdatedAt(now);
            audit.log(null, "PURCHASE_REQUEST_EXPIRED", "purchase_request", request.getId(), reason);
            events.publishEvent(new RealtimeEvent(
                List.of(request.getBuyer().getId(), request.getSeller().getId()),
                new UserEvent("REQUEST_EXPIRED", "purchase_request", request.getId(), request.getStatus())
            ));
        }
        return due.size();
    }

    @Transactional
    public int expirePickupWindows() {
        final OffsetDateTime now = OffsetDateTime.now();
        final List<Order> due = orders.findWindowExpired(OPEN_ORDERS, now);
        for (Order order : due) {
            order.setStatus(OrderStatus.NOT_PICKED_UP.name());
            order.setCodeHmac(null);
            audit.log(null, "ORDER_NOT_PICKED_UP", "order", order.getId());
            events.publishEvent(new RealtimeEvent(
                List.of(order.getBuyer().getId(), order.getSeller().getId()),
                new UserEvent("ORDER_NOT_PICKED_UP", "order", order.getId(), order.getStatus())
            ));
        }
        return due.size();
    }

    @Transactional
    public int purgePhotos() {
        final OffsetDateTime cutoff = OffsetDateTime.now().minusDays(PHOTO_RETENTION_DAYS);
        final List<Offer> purgeable = offers.findPhotoPurgeable(ALIVE_OFFERS, cutoff);
        for (Offer offer : purgeable) {
            photos.delete(offer.getPhotoKey());
            offer.setPhotoKey(null);
        }
        return purgeable.size();
    }

    @Transactional
    public int purgeAudit() {
        return auditEvents.deleteOlderThan(OffsetDateTime.now().minusMonths(AUDIT_RETENTION_MONTHS));
    }

    @Transactional
    public int purgeVerificationTokens() {
        return verificationTokens.deleteExpiredBefore(
            OffsetDateTime.now().minusDays(VERIFICATION_TOKEN_RETENTION_DAYS));
    }

    public int purgeRefreshTokens() {
        return refreshTokens.purgeExpired();
    }
}
