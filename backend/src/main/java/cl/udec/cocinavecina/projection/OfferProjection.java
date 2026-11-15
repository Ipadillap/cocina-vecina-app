package cl.udec.cocinavecina.projection;

import cl.udec.cocinavecina.model.Category;
import cl.udec.cocinavecina.model.OfferStatus;

import java.time.Instant;

public interface OfferProjection {
    Long getId();

    Long getSellerId();

    String getSeller();

    String getTitle();

    String getDescription();

    Category getCategory();

    String getPhotoKey();

    Integer getBasePrice();

    Integer getMinPrice();

    Integer getAvailablePortions();

    String getAllergens();

    Instant getPreparedAt();

    Instant getExpiresAt();

    OfferStatus getStatus();

    Double getCellLat();

    Double getCellLon();
}
