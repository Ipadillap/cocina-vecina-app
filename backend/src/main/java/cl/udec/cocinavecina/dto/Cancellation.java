package cl.udec.cocinavecina.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record Cancellation(
    @NotBlank
    @Size(max = 200)
    String reason
) {
}
