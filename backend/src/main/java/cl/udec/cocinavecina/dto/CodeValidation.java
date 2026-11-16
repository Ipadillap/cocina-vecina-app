package cl.udec.cocinavecina.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record CodeValidation(
    @NotBlank
    @Pattern(regexp = "^[0-9]{6}$")
    String code
) {
}
