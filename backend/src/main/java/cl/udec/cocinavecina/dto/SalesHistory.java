package cl.udec.cocinavecina.dto;

import java.time.Instant;
import java.util.List;

public record SalesHistory(
    String month,
    int offersPublished,
    int deliveredOrders,
    long totalIncome,
    List<Day> days,
    List<Sale> sales
) {
    public record Day(
        String date,
        int orders,
        long income
    ) {
    }

    public record Sale(
        long orderId,
        long offerId,
        String offerTitle,
        int unitPrice,
        int portions,
        long total,
        Instant deliveredAt
    ) {
    }
}
