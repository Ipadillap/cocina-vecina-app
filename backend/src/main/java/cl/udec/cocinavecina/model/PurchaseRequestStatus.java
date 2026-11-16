package cl.udec.cocinavecina.model;

public enum PurchaseRequestStatus {
    OFFER_SENT,
    COUNTEROFFER,
    ACCEPTED,
    REJECTED,
    CANCELLED,
    EXPIRED;

    public boolean isOpen() {
        return this == OFFER_SENT || this == COUNTEROFFER;
    }
}
