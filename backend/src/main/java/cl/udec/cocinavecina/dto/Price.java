package cl.udec.cocinavecina.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record Price(
    @NotNull
    @Positive
    Integer unitPrice
) {
}
