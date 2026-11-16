package cl.udec.cocinavecina.dto;

import jakarta.validation.constraints.NotBlank;

public record UploadRequest(
    @NotBlank
    String contentType
) {
}
