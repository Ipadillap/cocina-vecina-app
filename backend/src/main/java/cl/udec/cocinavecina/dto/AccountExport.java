package cl.udec.cocinavecina.dto;

import cl.udec.cocinavecina.model.AccountStatus;
import cl.udec.cocinavecina.model.Role;

import java.time.Instant;
import java.util.List;

public record AccountExport(
    Account account,
    List<OfferView> offers,
    List<OrderSummary> orders
) {
    public record Account(
        long id,
        String email,
        String displayName,
        String phone,
        Role role,
        AccountStatus status,
        boolean adultDeclared,
        Instant createdAt,
        Instant verifiedAt
    ) {}

    public record OrderSummary(
        long id,
        long offerId,
        String offerTitle,
        String side,
        int agreedPrice,
        int portions,
        String status,
        Instant acceptedAt
    ) {}
}
