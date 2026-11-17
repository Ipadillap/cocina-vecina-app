package cl.udec.cocinavecina.dto;

import jakarta.validation.constraints.NotNull;

public record ConsentChange(
    @NotNull(message = "Debe indicar si otorga o revoca el consentimiento.")
    Boolean granted
) {
}
