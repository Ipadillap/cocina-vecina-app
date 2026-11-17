package cl.udec.cocinavecina.service;

import cl.udec.cocinavecina.dto.AccountExport;
import cl.udec.cocinavecina.dto.RatingSummary;
import cl.udec.cocinavecina.dto.RatingView;
import cl.udec.cocinavecina.exception.ApiException;
import cl.udec.cocinavecina.model.AppUser;
import cl.udec.cocinavecina.model.Order;
import cl.udec.cocinavecina.model.OrderStatus;
import cl.udec.cocinavecina.model.Rating;
import cl.udec.cocinavecina.repository.AppUserRepository;
import cl.udec.cocinavecina.repository.OrdersRepository;
import cl.udec.cocinavecina.repository.RatingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;

@Service
public class RatingService {
    private static final Duration WINDOW = Duration.ofDays(7);

    private final OrdersRepository orders;
    private final RatingRepository ratings;
    private final AppUserRepository users;
    private final AuditService audit;

    public RatingService(
        OrdersRepository orders,
        RatingRepository ratings,
        AppUserRepository users,
        AuditService audit
    ) {
        this.orders = orders;
        this.ratings = ratings;
        this.users = users;
        this.audit = audit;
    }

    @Transactional
    public RatingView rate(Long userId, Long orderId, int score) {
        final Order order = orders.lockById(orderId)
            .orElseThrow(() -> ApiException.notFound("No existe el pedido " + orderId));
        final boolean isBuyer = order.getBuyer().getId().equals(userId);
        if (!isBuyer && !order.getSeller().getId().equals(userId)) {
            throw ApiException.forbidden("El pedido no pertenece al usuario");
        }
        if (!OrderStatus.DELIVERED.name().equals(order.getStatus()) || order.getDeliveredAt() == null) {
            throw ApiException.conflict("Solo se puede calificar un pedido Entregado");
        }
        final OffsetDateTime now = OffsetDateTime.now();
        if (now.isAfter(order.getDeliveredAt().plus(WINDOW))) {
            throw ApiException.rule("RF-14", "El plazo de 7 días para calificar terminó");
        }
        if (ratings.existsByOrder_IdAndAuthor_Id(orderId, userId)) {
            throw ApiException.conflict("Ya calificaste este pedido");
        }
        final AppUser author = isBuyer ? order.getBuyer() : order.getSeller();
        final AppUser recipient = isBuyer ? order.getSeller() : order.getBuyer();
        final Rating saved = ratings.save(Rating.builder()
            .order(order)
            .author(author)
            .recipient(recipient)
            .score(score)
            .createdAt(now)
            .build());
        audit.log(userId, "ORDER_RATED", "order", orderId, String.valueOf(score));
        return new RatingView(orderId, saved.getScore(), saved.getCreatedAt().toInstant());
    }

    @Transactional(readOnly = true)
    public RatingSummary summary(Long userId) {
        if (!users.existsById(userId)) {
            throw ApiException.notFound("No existe el usuario " + userId);
        }
        final long count = ratings.countByRecipient_Id(userId);
        final Double average = ratings.averageByRecipient(userId);
        return new RatingSummary(
            userId,
            count == 0 || average == null ? null : Math.round(average * 100.0) / 100.0,
            count
        );
    }

    @Transactional(readOnly = true)
    public List<AccountExport.RatingEntry> entriesOf(Long userId) {
        return ratings.findByAuthor_IdOrRecipient_IdOrderByCreatedAtAscIdAsc(userId, userId).stream()
            .map(r -> new AccountExport.RatingEntry(
                r.getOrder().getId(),
                r.getAuthor().getId().equals(userId) ? "GIVEN" : "RECEIVED",
                r.getScore(),
                r.getCreatedAt().toInstant()
            ))
            .toList();
    }
}
