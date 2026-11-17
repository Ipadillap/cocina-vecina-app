package cl.udec.cocinavecina.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record NewPurchaseRequest(
    @NotNull
    @Positive
    Integer portions,
    @Positive
    Integer unitPrice
) {
}
