package cl.udec.cocinavecina.dto;

import java.time.Instant;

public record CodeView(
    Long orderId,
    String code,
    Instant validUntil
) {
}
