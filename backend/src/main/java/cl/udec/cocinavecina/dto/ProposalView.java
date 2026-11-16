package cl.udec.cocinavecina.dto;

import cl.udec.cocinavecina.model.Party;

import java.time.Instant;

public record ProposalView(
    Party party,
    int unitPrice,
    Instant createdAt
) {
}
