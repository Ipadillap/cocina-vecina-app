package cl.udec.cocinavecina.dto;

public record UserEvent(
    String type,
    String entity,
    long entityId,
    String status
) {
}
