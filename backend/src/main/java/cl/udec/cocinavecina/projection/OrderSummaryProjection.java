package cl.udec.cocinavecina.projection;

import java.time.Instant;

public interface OrderSummaryProjection {
    Long getId();

    Long getOfferId();

    String getOfferTitle();

    String getSide();

    Integer getAgreedPrice();

    Integer getPortions();

    String getStatus();

    Instant getAcceptedAt();
}
