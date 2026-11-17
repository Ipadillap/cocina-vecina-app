package cl.udec.cocinavecina.service;

import cl.udec.cocinavecina.dto.SalesHistory;
import cl.udec.cocinavecina.projection.SaleProjection;
import cl.udec.cocinavecina.repository.OfferRepository;
import cl.udec.cocinavecina.repository.OrdersRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
public class SalesHistoryService {
    private static final ZoneId ZONE = ZoneId.of("America/Santiago");

    private final OrdersRepository orders;
    private final OfferRepository offers;

    public SalesHistoryService(OrdersRepository orders, OfferRepository offers) {
        this.orders = orders;
        this.offers = offers;
    }

    public static YearMonth currentMonth() {
        return YearMonth.now(ZONE);
    }

    @Transactional(readOnly = true)
    public SalesHistory month(Long sellerId, YearMonth month) {
        final OffsetDateTime from = month.atDay(1).atStartOfDay(ZONE).toOffsetDateTime();
        final OffsetDateTime to = month.plusMonths(1).atDay(1).atStartOfDay(ZONE).toOffsetDateTime();
        final List<SaleProjection> delivered = orders.findDeliveredBySeller(sellerId, from, to);
        final long published = offers
            .countBySeller_IdAndPublishedAtGreaterThanEqualAndPublishedAtLessThan(sellerId, from, to);

        final List<SalesHistory.Sale> sales = delivered.stream()
            .map(s -> new SalesHistory.Sale(
                s.getId(),
                s.getOfferId(),
                s.getOfferTitle(),
                s.getAgreedPrice(),
                s.getPortions(),
                (long) s.getAgreedPrice() * s.getPortions(),
                s.getDeliveredAt()
            ))
            .toList();

        final Map<LocalDate, long[]> perDay = new TreeMap<>();
        for (SalesHistory.Sale sale : sales) {
            final LocalDate day = sale.deliveredAt().atZone(ZONE).toLocalDate();
            final long[] totals = perDay.computeIfAbsent(day, d -> new long[2]);
            totals[0] += 1;
            totals[1] += sale.total();
        }
        final List<SalesHistory.Day> days = perDay.entrySet().stream()
            .map(e -> new SalesHistory.Day(e.getKey().toString(), (int) e.getValue()[0], e.getValue()[1]))
            .toList();

        return new SalesHistory(
            month.toString(),
            (int) published,
            sales.size(),
            sales.stream().mapToLong(SalesHistory.Sale::total).sum(),
            days,
            sales
        );
    }
}
