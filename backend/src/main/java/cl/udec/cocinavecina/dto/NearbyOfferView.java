package cl.udec.cocinavecina.dto;

import cl.udec.cocinavecina.model.Category;

import java.time.Instant;

public record NearbyOfferView(
    long id,
    String title,
    Category category,
    String photoUrl,
    int basePrice,
    int minPrice,
    int availablePortions,
    Instant expiresAt,
    double approxLatitude,
    double approxLongitude,
    long distanceMeters
) {}
