package cl.udec.cocinavecina.projection;

import java.time.Instant;

public interface SaleProjection {
    Long getId();

    Long getOfferId();

    String getOfferTitle();

    Integer getAgreedPrice();

    Integer getPortions();

    Instant getDeliveredAt();
}
