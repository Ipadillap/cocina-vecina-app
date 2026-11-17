package cl.udec.cocinavecina.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record NewRating(
    @NotNull(message = "El puntaje es obligatorio.")
    @Min(value = 1, message = "El puntaje mínimo es 1.")
    @Max(value = 5, message = "El puntaje máximo es 5.")
    Integer score
) {
}
