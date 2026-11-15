package cl.udec.cocinavecina.dto;

import cl.udec.cocinavecina.model.Allergen;
import cl.udec.cocinavecina.model.Category;
import cl.udec.cocinavecina.model.RiskIngredient;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.Set;

public record NewOfferRequest(
    @NotNull Long pickupPointId,
    @NotBlank @Size(max = 80) String title,
    @Size(max = 300) String description,
    @NotNull Category category,
    @Size(max = 200) String photoKey,
    @Positive int basePrice,
    @Positive int minPrice,
    @Positive int portions,
    @NotEmpty Set<Allergen> allergens,
    Set<RiskIngredient> excludedIngredients,
    @NotNull Instant preparedAt,
    @NotNull Instant expiresAt
) {}
