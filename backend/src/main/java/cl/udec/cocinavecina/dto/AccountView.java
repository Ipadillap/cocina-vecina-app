package cl.udec.cocinavecina.dto;

import cl.udec.cocinavecina.model.Role;

import java.time.Instant;

public record AccountView(
    long id,
    String email,
    String displayName,
    String phone,
    Role role,
    boolean verified,
    String termsVersion,
    Instant termsAcceptedAt
) {
}
