package cl.udec.cocinavecina.projection;

import cl.udec.cocinavecina.model.Category;

import java.time.Instant;

public interface NearbyOfferProjection {
    Long getId();

    String getTitle();

    Category getCategory();

    String getPhotoKey();

    Integer getBasePrice();

    Integer getMinPrice();

    Integer getAvailablePortions();

    Instant getExpiresAt();

    Double getCellLat();

    Double getCellLon();

    Double getDistance();
}
