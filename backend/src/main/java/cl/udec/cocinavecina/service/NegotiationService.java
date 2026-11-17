package cl.udec.cocinavecina.service;

import cl.udec.cocinavecina.components.RealtimeEvent;
import cl.udec.cocinavecina.dto.UserEvent;
import org.springframework.context.ApplicationEventPublisher;
import cl.udec.cocinavecina.dto.NewPurchaseRequest;
import cl.udec.cocinavecina.dto.ProposalView;
import cl.udec.cocinavecina.dto.PurchaseRequestView;
import cl.udec.cocinavecina.exception.ApiException;
import cl.udec.cocinavecina.model.AccountStatus;
import cl.udec.cocinavecina.model.AppUser;
import cl.udec.cocinavecina.model.Offer;
import cl.udec.cocinavecina.model.OfferStatus;
import cl.udec.cocinavecina.model.Order;
import cl.udec.cocinavecina.model.OrderStatus;
import cl.udec.cocinavecina.model.Party;
import cl.udec.cocinavecina.model.Proposal;
import cl.udec.cocinavecina.model.PurchaseRequest;
import cl.udec.cocinavecina.model.PurchaseRequestStatus;
import cl.udec.cocinavecina.repository.AppUserRepository;
import cl.udec.cocinavecina.repository.OfferRepository;
import cl.udec.cocinavecina.repository.OrdersRepository;
import cl.udec.cocinavecina.repository.ProposalRepository;
import cl.udec.cocinavecina.repository.PurchaseRequestRepository;
import cl.udec.cocinavecina.rules.NegotiationRules;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class NegotiationService {

    private static final Duration PICKUP_WINDOW = Duration.ofHours(2);
    private static final List<String> OPEN_STATUSES = List.of(
        PurchaseRequestStatus.OFFER_SENT.name(),
        PurchaseRequestStatus.COUNTEROFFER.name()
    );

    private final PurchaseRequestRepository requests;
    private final ProposalRepository proposals;
    private final OfferRepository offers;
    private final OrdersRepository orders;
    private final AppUserRepository users;
    private final AuditService audit;
    private final ApplicationEventPublisher events;

    public NegotiationService(
        PurchaseRequestRepository requests,
        ProposalRepository proposals,
        OfferRepository offers,
        OrdersRepository orders,
        AppUserRepository users,
        AuditService audit,
        ApplicationEventPublisher events
    ) {
        this.requests = requests;
        this.proposals = proposals;
        this.offers = offers;
        this.orders = orders;
        this.users = users;
        this.audit = audit;
        this.events = events;
    }


    @Transactional
    public PurchaseRequestView submit(Long buyerId, Long offerId, NewPurchaseRequest body) {
        final AppUser buyer = requireActive(buyerId);
        final Offer offer = offers.lockById(offerId)
            .orElseThrow(() -> ApiException.notFound("No existe la oferta " + offerId));
        if (offer.getSeller().getId().equals(buyerId)) {
            throw ApiException.rule("RF-5", "No puedes solicitar porciones de tu propia oferta");
        }
        final OffsetDateTime now = OffsetDateTime.now();
        if (offer.getStatus() != OfferStatus.ACTIVE || !now.isBefore(offer.getExpiresAt())) {
            throw ApiException.conflict("La oferta ya no está disponible");
        }
        if (requests.existsByOffer_IdAndBuyer_IdAndStatusIn(offerId, buyerId, OPEN_STATUSES)) {
            throw ApiException.conflict("Ya tienes una solicitud abierta para esta oferta");
        }
        NegotiationRules.validatePortions(body.portions(), offer.getAvailablePortions());
        final int unitPrice = body.unitPrice() == null ? offer.getBasePrice() : body.unitPrice();
        NegotiationRules.validateBuyerProposal(unitPrice, offer.getMinPrice(), offer.getBasePrice());

        final PurchaseRequest request = requests.save(PurchaseRequest.builder()
            .offer(offer)
            .seller(offer.getSeller())
            .buyer(buyer)
            .portions(body.portions())
            .rounds(0)
            .currentPrice(unitPrice)
            .currentParty(Party.BUYER.name())
            .status(PurchaseRequestStatus.OFFER_SENT.name())
            .offerExpiresAt(offer.getExpiresAt())
            .createdAt(now)
            .updatedAt(now)
            .build());
        addProposal(request, Party.BUYER, unitPrice, now);
        audit.log(buyerId, "PURCHASE_REQUEST_SENT", "purchase_request", request.getId());
        notifyParties(request, "PROPOSAL_RECEIVED");
        return view(request);
    }

    @Transactional(readOnly = true)
    public List<PurchaseRequestView> pending(Long sellerId) {
        return views(requests.findBySeller_IdAndStatusOrderByCreatedAtAsc(
            sellerId, PurchaseRequestStatus.OFFER_SENT.name()));
    }

    @Transactional(readOnly = true)
    public List<PurchaseRequestView> mine(Long buyerId) {
        return views(requests.findByBuyer_IdOrderByUpdatedAtDesc(buyerId));
    }

    @Transactional(readOnly = true)
    public PurchaseRequestView view(Long userId, Long requestId) {
        final PurchaseRequest request = requests.findById(requestId)
            .orElseThrow(() -> ApiException.notFound("No existe la solicitud " + requestId));
        partyOf(request, userId);
        return view(request);
    }

    /** RF-7, RB-2 y RB-3: el vendedor responde con un precio entre la propuesta vigente y el base. */
    @Transactional
    public PurchaseRequestView counteroffer(Long sellerId, Long requestId, int unitPrice) {
        final PurchaseRequest request = lock(requestId);
        if (!request.getSeller().getId().equals(sellerId)) {
            throw ApiException.forbidden("Solo el vendedor de la oferta puede contraofertar");
        }
        requireTurn(request, Party.SELLER);
        NegotiationRules.validateRoundAvailable(request.getRounds());
        NegotiationRules.validateCounteroffer(
            unitPrice, request.getCurrentPrice(), request.getOffer().getBasePrice());

        request.setRounds(request.getRounds() + 1);
        move(request, Party.SELLER, unitPrice);
        audit.log(sellerId, "COUNTEROFFER_SENT", "purchase_request", requestId);
        notifyParties(request, "COUNTEROFFER_RECEIVED");
        return view(request);
    }

    /** RF-6: el comprador responde a una contraoferta con otro precio. */
    @Transactional
    public PurchaseRequestView propose(Long buyerId, Long requestId, int unitPrice) {
        final PurchaseRequest request = lock(requestId);
        if (!request.getBuyer().getId().equals(buyerId)) {
            throw ApiException.forbidden("Solo el comprador de la solicitud puede proponer un precio");
        }
        requireTurn(request, Party.BUYER);
        NegotiationRules.validateRoundAvailable(request.getRounds());
        final Offer offer = request.getOffer();
        NegotiationRules.validateBuyerProposal(unitPrice, offer.getMinPrice(), offer.getBasePrice());

        move(request, Party.BUYER, unitPrice);
        audit.log(buyerId, "PROPOSAL_SENT", "purchase_request", requestId);
        notifyParties(request, "PROPOSAL_RECEIVED");
        return view(request);
    }

    /**
     * RF-7: quien tiene el turno acepta el precio vigente; se descuenta el stock y se crea el pedido.
     * Si no alcanza el stock, la solicitud queda EXPIRED y se responde 409 (RNF-R-4); por eso esa
     * excepción no revierte la transacción.
     */
    @Transactional(noRollbackFor = ApiException.class)
    public PurchaseRequestView accept(Long userId, Long requestId) {
        final PurchaseRequest request = lock(requestId);
        requireTurn(request, partyOf(request, userId));
        final Offer offer = request.getOffer();
        if (offer.getStatus() != OfferStatus.ACTIVE) {
            throw ApiException.conflict("La oferta ya no está disponible");
        }
        final OffsetDateTime now = OffsetDateTime.now();
        if (offers.reserve(offer.getId(), request.getPortions()) == 0) {
            close(request, PurchaseRequestStatus.EXPIRED, "Stock agotado", now);
            audit.log(userId, "PURCHASE_REQUEST_EXPIRED", "purchase_request", requestId, "Stock agotado");
            notifyParties(request, "REQUEST_EXPIRED");
            throw ApiException.conflict("No quedan porciones suficientes; la solicitud expiró");
        }

        final Order order = orders.save(Order.builder()
            .request(request)
            .offer(offer)
            .buyer(request.getBuyer())
            .seller(request.getSeller())
            .agreedPrice(request.getCurrentPrice())
            .portions(request.getPortions())
            .status(OrderStatus.ACCEPTED.name())
            .acceptedAt(now)
            .windowEnd(now.plus(PICKUP_WINDOW))
            .codeAttempts(0)
            .codeIssues(0)
            .build());
        close(request, PurchaseRequestStatus.ACCEPTED, null, now);
        audit.log(userId, "PURCHASE_REQUEST_ACCEPTED", "purchase_request", requestId);
        audit.log(userId, "ORDER_CREATED", "order", order.getId());
        notifyParties(request, "REQUEST_ACCEPTED");
        events.publishEvent(new RealtimeEvent(
            List.of(request.getBuyer().getId(), request.getSeller().getId()),
            new UserEvent("ORDER_CREATED", "order", order.getId(), order.getStatus())));
        return view(request);
    }

    /** RF-7: quien tiene el turno rechaza la propuesta vigente y la negociación termina. */
    @Transactional
    public PurchaseRequestView reject(Long userId, Long requestId) {
        final PurchaseRequest request = lock(requestId);
        final Party party = partyOf(request, userId);
        requireTurn(request, party);
        final String reason = party == Party.SELLER ? "Rechazada por el vendedor" : "Rechazada por el comprador";
        close(request, PurchaseRequestStatus.REJECTED, reason, OffsetDateTime.now());
        audit.log(userId, "PURCHASE_REQUEST_REJECTED", "purchase_request", requestId);
        notifyParties(request, "REQUEST_REJECTED");
        return view(request);
    }

    /** El comprador retira su solicitud abierta, sin importar de quién sea el turno. */
    @Transactional
    public PurchaseRequestView withdraw(Long buyerId, Long requestId) {
        final PurchaseRequest request = lock(requestId);
        if (!request.getBuyer().getId().equals(buyerId)) {
            throw ApiException.forbidden("Solo el comprador puede retirar su solicitud");
        }
        requireOpen(request);
        close(request, PurchaseRequestStatus.CANCELLED, "Retirada por el comprador", OffsetDateTime.now());
        audit.log(buyerId, "PURCHASE_REQUEST_WITHDRAWN", "purchase_request", requestId);
        notifyParties(request, "REQUEST_WITHDRAWN");
        return view(request);
    }

    private void notifyParties(PurchaseRequest request, String type) {
        events.publishEvent(new RealtimeEvent(
            List.of(request.getBuyer().getId(), request.getSeller().getId()),
            new UserEvent(type, "purchase_request", request.getId(), request.getStatus())));
    }

    private PurchaseRequest lock(Long requestId) {
        return requests.lockById(requestId)
            .orElseThrow(() -> ApiException.notFound("No existe la solicitud " + requestId));
    }

    private AppUser requireActive(Long userId) {
        final AppUser user = users.findById(userId)
            .orElseThrow(() -> ApiException.notFound("No existe el usuario " + userId));
        if (user.getStatus() != AccountStatus.ACTIVE) {
            throw ApiException.forbidden("La cuenta debe estar verificada y activa");
        }
        return user;
    }

    private static Party partyOf(PurchaseRequest request, Long userId) {
        if (request.getBuyer().getId().equals(userId)) {
            return Party.BUYER;
        }
        if (request.getSeller().getId().equals(userId)) {
            return Party.SELLER;
        }
        throw ApiException.forbidden("La solicitud no pertenece al usuario");
    }

    private static void requireOpen(PurchaseRequest request) {
        final PurchaseRequestStatus status = PurchaseRequestStatus.valueOf(request.getStatus());
        if (!status.isOpen()) {
            throw ApiException.conflict("La solicitud ya está cerrada (" + status + ")");
        }
        if (!OffsetDateTime.now().isBefore(request.getOfferExpiresAt())) {
            throw ApiException.conflict("La oferta de esta solicitud ya expiró");
        }
    }

    private static void requireTurn(PurchaseRequest request, Party party) {
        requireOpen(request);
        if (turn(request) != party) {
            throw ApiException.conflict("No es tu turno en esta negociación");
        }
    }

    /** Responde la parte contraria a la que hizo la última propuesta; nadie, si la solicitud está cerrada. */
    private static Party turn(PurchaseRequest request) {
        return PurchaseRequestStatus.valueOf(request.getStatus()).isOpen()
            ? Party.valueOf(request.getCurrentParty()).other()
            : null;
    }

    private void move(PurchaseRequest request, Party party, int unitPrice) {
        final OffsetDateTime now = OffsetDateTime.now();
        request.setCurrentPrice(unitPrice);
        request.setCurrentParty(party.name());
        request.setStatus(party == Party.SELLER
            ? PurchaseRequestStatus.COUNTEROFFER.name()
            : PurchaseRequestStatus.OFFER_SENT.name());
        request.setUpdatedAt(now);
        addProposal(request, party, unitPrice, now);
    }

    private void addProposal(PurchaseRequest request, Party party, int unitPrice, OffsetDateTime now) {
        proposals.save(Proposal.builder()
            .request(request)
            .party(party.name())
            .unitPrice(unitPrice)
            .createdAt(now)
            .build());
    }

    private static void close(PurchaseRequest request,
                              PurchaseRequestStatus status,
                              String reason,
                              OffsetDateTime now) {
        request.setStatus(status.name());
        request.setCloseReason(reason);
        request.setUpdatedAt(now);
    }

    private PurchaseRequestView view(PurchaseRequest request) {
        return views(List.of(request)).getFirst();
    }

    /** Arma las vistas con dos consultas en total (propuestas y pedidos), no dos por solicitud. */
    private List<PurchaseRequestView> views(List<PurchaseRequest> list) {
        if (list.isEmpty()) {
            return List.of();
        }
        final List<Long> ids = list.stream().map(PurchaseRequest::getId).toList();
        final Map<Long, List<ProposalView>> history = proposals.findByRequest_IdInOrderByCreatedAtAscIdAsc(ids)
            .stream()
            .collect(Collectors.groupingBy(
                p -> p.getRequest().getId(),
                Collectors.mapping(NegotiationService::toProposalView, Collectors.toList())
            ));
        final Map<Long, Long> orderIds = orders.findByRequest_IdIn(ids).stream()
            .collect(Collectors.toMap(o -> o.getRequest().getId(), Order::getId));
        return list.stream()
            .map(r -> toView(r, history.getOrDefault(r.getId(), List.of()), orderIds.get(r.getId())))
            .toList();
    }

    private static PurchaseRequestView toView(PurchaseRequest r, List<ProposalView> history, Long orderId) {
        return new PurchaseRequestView(
            r.getId(),
            r.getOffer().getId(),
            r.getPortions(),
            r.getRounds(),
            r.getCurrentPrice(),
            Party.valueOf(r.getCurrentParty()),
            turn(r),
            PurchaseRequestStatus.valueOf(r.getStatus()),
            r.getCloseReason(),
            orderId,
            history,
            r.getBuyer().getDisplayName()
        );
    }

    private static ProposalView toProposalView(Proposal p) {
        return new ProposalView(Party.valueOf(p.getParty()), p.getUnitPrice(), p.getCreatedAt().toInstant());
    }
}
