package cl.udec.cocinavecina.dto;

import cl.udec.cocinavecina.model.Allergen;
import cl.udec.cocinavecina.model.Category;
import cl.udec.cocinavecina.model.OfferStatus;

import java.time.Instant;
import java.util.Set;

public record OfferView(
    long id,
    long sellerId,
    String seller,
    String title,
    String description,
    Category category,
    String photoUrl,
    int basePrice,
    int minPrice,
    int availablePortions,
    Set<Allergen> allergens,
    Instant preparedAt,
    Instant expiresAt,
    OfferStatus status,
    double approxLatitude,
    double approxLongitude
) {}
