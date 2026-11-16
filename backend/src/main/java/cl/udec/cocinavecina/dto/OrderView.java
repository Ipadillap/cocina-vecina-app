package cl.udec.cocinavecina.dto;

import java.time.Instant;

public record OrderView(
    Long id,
    Long offerId,
    Long buyerId,
    Long sellerId,
    int agreedPrice,
    int portions,
    String status,
    Instant acceptedAt,
    Instant readyAt,
    Instant deliveredAt,
    Instant windowEnd
) {
}
