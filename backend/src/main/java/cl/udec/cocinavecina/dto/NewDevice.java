package cl.udec.cocinavecina.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NewDevice(
    @NotBlank
    @Size(max = 512)
    String pushToken
) {
}
