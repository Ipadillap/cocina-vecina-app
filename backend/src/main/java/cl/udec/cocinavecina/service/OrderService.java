package cl.udec.cocinavecina.service;

import cl.udec.cocinavecina.components.FieldEncryptor;
import cl.udec.cocinavecina.dto.CodeView;
import cl.udec.cocinavecina.dto.OrderView;
import cl.udec.cocinavecina.dto.PickupDetailsView;
import cl.udec.cocinavecina.exception.ApiException;
import cl.udec.cocinavecina.model.Order;
import cl.udec.cocinavecina.model.OrderStatus;
import cl.udec.cocinavecina.model.PickupPoint;
import cl.udec.cocinavecina.repository.OfferRepository;
import cl.udec.cocinavecina.repository.OrdersRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.List;

@Service
public class OrderService {
    private static final Duration UNLOCK_AFTER_DELIVERY = Duration.ofHours(1);
    private static final int MAX_CODE_ISSUES = 2;
    private static final int MAX_CODE_ATTEMPTS = 5;

    private final OrdersRepository orders;
    private final OfferRepository offers;
    private final FieldEncryptor encryptor;
    private final AuditService audit;
    private final byte[] codeKey;
    private final SecureRandom random = new SecureRandom();

    public OrderService(
        OrdersRepository orders,
        OfferRepository offers,
        FieldEncryptor encryptor,
        AuditService audit,
        @Value("${cocinavecina.code.hmac-key}") String codeKey
    ) {
        this.orders = orders;
        this.offers = offers;
        this.encryptor = encryptor;
        this.audit = audit;
        this.codeKey = codeKey.getBytes(StandardCharsets.UTF_8);
    }

    @Transactional(readOnly = true)
    public List<OrderView> mine(Long userId) {
        return orders.findByBuyer_IdOrSeller_IdOrderByAcceptedAtDesc(userId, userId).stream()
            .map(OrderService::toView)
            .toList();
    }

    @Transactional(readOnly = true)
    public OrderView view(Long userId, Long orderId) {
        final Order order = find(orderId);
        requireParty(order, userId);
        return toView(order);
    }

    @Transactional
    public PickupDetailsView pickup(Long userId, Long orderId) {
        final Order order = find(orderId);
        if (!order.getBuyer().getId().equals(userId)) {
            throw ApiException.forbidden("Solo el comprador del pedido puede ver los datos de retiro");
        }
        final OrderStatus status = OrderStatus.valueOf(order.getStatus());
        final boolean open = status == OrderStatus.ACCEPTED || status == OrderStatus.READY_FOR_PICKUP;
        final boolean recentlyDelivered = status == OrderStatus.DELIVERED
            && order.getDeliveredAt() != null
            && OffsetDateTime.now().isBefore(order.getDeliveredAt().plus(UNLOCK_AFTER_DELIVERY));
        if (!open && !recentlyDelivered) {
            throw ApiException.forbidden("Los datos de retiro ya no están disponibles para este pedido");
        }
        final PickupPoint point = order.getOffer().getPickupPoint();
        final PickupDetailsView view = new PickupDetailsView(
            encryptor.decrypt(point.getEncryptedAddress()),
            encryptor.decrypt(point.getEncryptedInstructions()),
            Double.parseDouble(encryptor.decrypt(point.getEncryptedLat())),
            Double.parseDouble(encryptor.decrypt(point.getEncryptedLon())),
            encryptor.decrypt(order.getSeller().getEncryptedPhone())
        );
        audit.log(userId, "PICKUP_DATA_ACCESSED", "order", orderId);
        return view;
    }

    @Transactional
    public CodeView issueCode(Long userId, Long orderId) {
        final Order order = lock(orderId);
        if (!order.getBuyer().getId().equals(userId)) {
            throw ApiException.forbidden("Solo el comprador puede emitir el código de entrega");
        }
        requireOpen(order);
        if (order.getCodeIssues() >= MAX_CODE_ISSUES) {
            throw ApiException.conflict("Se alcanzó el máximo de códigos emitidos para este pedido");
        }
        final String code = "%06d".formatted(random.nextInt(1_000_000));
        order.setCodeHmac(hmac(orderId, code));
        order.setCodeAttempts(0);
        order.setCodeIssues(order.getCodeIssues() + 1);
        audit.log(userId, "DELIVERY_CODE_ISSUED", "order", orderId);
        return new CodeView(orderId, code, order.getWindowEnd().toInstant());
    }

    @Transactional
    public OrderView markReady(Long userId, Long orderId) {
        final Order order = lock(orderId);
        if (!order.getSeller().getId().equals(userId)) {
            throw ApiException.forbidden("Solo el vendedor puede marcar el pedido como listo");
        }
        if (!OrderStatus.ACCEPTED.name().equals(order.getStatus())) {
            throw ApiException.conflict("Solo un pedido Aceptado puede pasar a Listo para retiro");
        }
        order.setStatus(OrderStatus.READY_FOR_PICKUP.name());
        order.setReadyAt(OffsetDateTime.now());
        audit.log(userId, "ORDER_READY", "order", orderId);
        return toView(order);
    }

    /**
     * Los intentos fallidos deben persistir aunque se responda 422, por eso no se revierte la transacción.
     */
    @Transactional(noRollbackFor = ApiException.class)
    public OrderView validateDelivery(Long userId, Long orderId, String code) {
        final Order order = lock(orderId);
        if (!order.getSeller().getId().equals(userId)) {
            throw ApiException.forbidden("Solo el vendedor puede validar la entrega");
        }
        if (!OrderStatus.READY_FOR_PICKUP.name().equals(order.getStatus())) {
            throw ApiException.conflict("El pedido debe estar Listo para retiro");
        }
        if (OffsetDateTime.now().isAfter(order.getWindowEnd())) {
            throw ApiException.conflict("La ventana de retiro terminó");
        }
        if (order.getCodeHmac() == null) {
            throw ApiException.conflict("No hay un código vigente; el comprador debe emitir uno nuevo");
        }
        final boolean matches = MessageDigest.isEqual(
            order.getCodeHmac().getBytes(StandardCharsets.UTF_8),
            hmac(orderId, code).getBytes(StandardCharsets.UTF_8)
        );
        if (!matches) {
            final int attempts = order.getCodeAttempts() + 1;
            order.setCodeAttempts(attempts);
            audit.log(userId, "DELIVERY_CODE_FAILED", "order", orderId);
            if (attempts >= MAX_CODE_ATTEMPTS) {
                order.setCodeHmac(null);
                throw ApiException.rule(
                    "RF-15",
                    "Código incorrecto. Se agotaron los intentos y el código quedó invalidado"
                );
            }
            throw ApiException.rule(
                "RF-15",
                "Código incorrecto. Intentos restantes: " + (MAX_CODE_ATTEMPTS - attempts)
            );
        }
        order.setStatus(OrderStatus.DELIVERED.name());
        order.setDeliveredAt(OffsetDateTime.now());
        order.setCodeHmac(null);
        audit.log(userId, "ORDER_DELIVERED", "order", orderId);
        return toView(order);
    }

    @Transactional
    public OrderView cancel(Long userId, Long orderId, String reason) {
        final Order order = lock(orderId);
        requireParty(order, userId);
        if (!OrderStatus.ACCEPTED.name().equals(order.getStatus())) {
            throw ApiException.conflict("Solo un pedido Aceptado puede cancelarse");
        }
        order.setStatus(OrderStatus.CANCELLED.name());
        order.setCancelledBy(order.getBuyer().getId().equals(userId) ? order.getBuyer() : order.getSeller());
        order.setCancellationReason(reason.trim());
        order.setCancelledAt(OffsetDateTime.now());
        order.setCodeHmac(null);
        offers.restock(order.getOffer().getId(), order.getPortions());
        audit.log(userId, "ORDER_CANCELLED", "order", orderId, reason.trim());
        return toView(order);
    }

    private Order find(Long orderId) {
        return orders.findById(orderId)
            .orElseThrow(() -> ApiException.notFound("No existe el pedido " + orderId));
    }

    private Order lock(Long orderId) {
        return orders.lockById(orderId)
            .orElseThrow(() -> ApiException.notFound("No existe el pedido " + orderId));
    }

    private static void requireParty(Order order, Long userId) {
        if (!order.getBuyer().getId().equals(userId) && !order.getSeller().getId().equals(userId)) {
            throw ApiException.forbidden("El pedido no pertenece al usuario");
        }
    }

    private static void requireOpen(Order order) {
        final String status = order.getStatus();
        if (!OrderStatus.ACCEPTED.name().equals(status) && !OrderStatus.READY_FOR_PICKUP.name().equals(status)) {
            throw ApiException.conflict("El pedido no admite un código de entrega en estado " + status);
        }
        if (OffsetDateTime.now().isAfter(order.getWindowEnd())) {
            throw ApiException.conflict("La ventana de retiro terminó");
        }
    }

    private String hmac(Long orderId, String code) {
        try {
            final Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(codeKey, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal((orderId + ":" + code).getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo calcular el HMAC del código", e);
        }
    }

    private static OrderView toView(Order o) {
        return new OrderView(
            o.getId(),
            o.getOffer().getId(),
            o.getBuyer().getId(),
            o.getSeller().getId(),
            o.getAgreedPrice(),
            o.getPortions(),
            o.getStatus(),
            o.getAcceptedAt().toInstant(),
            o.getReadyAt() == null ? null : o.getReadyAt().toInstant(),
            o.getDeliveredAt() == null ? null : o.getDeliveredAt().toInstant(),
            o.getWindowEnd().toInstant()
        );
    }
}
