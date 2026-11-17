package cl.udec.cocinavecina.dto;

import cl.udec.cocinavecina.model.ConsentType;

import java.time.Instant;

public record ConsentView(
    ConsentType type,
    boolean granted,
    Instant grantedAt,
    Instant revokedAt,
    String textVersion
) {
}
