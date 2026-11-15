package cl.udec.cocinavecina.dto;

public record PickUpPointView(
    long id,
    String alias,
    double approxLatitude,
    double approxLongitude
) {}
