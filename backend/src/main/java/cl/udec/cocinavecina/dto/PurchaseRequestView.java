package cl.udec.cocinavecina.dto;

import cl.udec.cocinavecina.model.Party;
import cl.udec.cocinavecina.model.PurchaseRequestStatus;

import java.util.List;

public record PurchaseRequestView(
    Long id,
    Long offerId,
    int portions,
    int rounds,
    int currentPrice,
    Party currentParty,
    Party turn,
    PurchaseRequestStatus status,
    String closeReason,
    Long orderId,
    List<ProposalView> proposals,
    String buyerName
) {
}
