package cl.udec.cocinavecina.dto;

import java.util.List;

public record OfferUnderReview(
    Long offerId,
    String title,
    Long sellerId,
    long sellerReportCount,
    List<ReportView> reports
) {
}
