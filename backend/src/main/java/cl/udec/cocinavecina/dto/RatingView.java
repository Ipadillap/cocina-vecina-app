package cl.udec.cocinavecina.dto;

import java.time.Instant;

public record RatingView(
    long orderId,
    int score,
    Instant createdAt
) {
}
