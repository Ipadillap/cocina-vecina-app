package cl.udec.cocinavecina.dto;

import cl.udec.cocinavecina.model.ReportReason;

import java.time.Instant;

public record ReportView(
    ReportReason reason,
    String detail,
    Instant createdAt
) {
}
