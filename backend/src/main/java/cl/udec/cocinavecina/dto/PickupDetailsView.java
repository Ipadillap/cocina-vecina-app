package cl.udec.cocinavecina.dto;

public record PickupDetailsView(
    String address,
    String instructions,
    double latitude,
    double longitude,
    String phone
) {
}
