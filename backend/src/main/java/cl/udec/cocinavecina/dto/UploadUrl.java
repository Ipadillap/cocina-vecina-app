package cl.udec.cocinavecina.dto;

public record UploadUrl(
    String key,
    String url,
    String method,
    String contentType
) {
}
