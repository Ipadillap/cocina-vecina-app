package cl.udec.cocinavecina.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AccountDecision(
    @NotNull
    Action action,
    @NotBlank
    @Size(max = 200)
    String reason
) {
    public enum Action {
        SUSPEND,
        REACTIVATE
    }
}
