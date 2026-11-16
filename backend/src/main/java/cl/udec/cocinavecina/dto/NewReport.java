package cl.udec.cocinavecina.dto;

import cl.udec.cocinavecina.model.ReportReason;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record NewReport(
    @NotNull
    ReportReason reason,
    @Size(max = 300)
    String detail
) {
}
