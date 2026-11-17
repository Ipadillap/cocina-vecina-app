package cl.udec.cocinavecina.dto;

public record RatingSummary(
    long userId,
    Double average,
    long count
) {
}
