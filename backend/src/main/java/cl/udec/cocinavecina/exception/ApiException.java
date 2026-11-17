package cl.udec.cocinavecina.exception;

import cl.udec.cocinavecina.dto.ApiResponse;
import cl.udec.cocinavecina.dto.HTTP;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
public class ApiException extends RuntimeException {
    private final ApiResponse response;

    @Builder
    public ApiException(String title,
                        HTTP.Response status,
                        String message,
                        List<String> detail) {
        super(message);
        this.response = new ApiResponse(title, status.status(), detail);
    }

    public static ApiException badRequest(String... detail) {
        return of("Solicitud inválida", HTTP.Response.BAD_REQUEST, detail);
    }

    public static ApiException unauthorized(String... detail) {
        return of("No autenticado", HTTP.Response.UNAUTHORIZED, detail);
    }

    public static ApiException forbidden(String... detail) {
        return of("Prohibido", HTTP.Response.FORBIDDEN, detail);
    }

    public static ApiException notFound(String... detail) {
        return of("No encontrado", HTTP.Response.NOT_FOUND, detail);
    }

    public static ApiException conflict(String... detail) {
        return of("Conflicto", HTTP.Response.CONFLICT, detail);
    }

    public static ApiException invalid(String... detail) {
        return of("Datos inválidos", HTTP.Response.UNPROCESSABLE_ENTITY, detail);
    }

    public static ApiException rule(String rule, String... detail) {
        return of(rule, HTTP.Response.UNPROCESSABLE_ENTITY, detail);
    }

    private static ApiException of(String title, HTTP.Response status, String... detail) {
        return ApiException.builder()
            .title(title)
            .status(status)
            .message(String.join(" ", detail))
            .detail(List.of(detail))
            .build();
    }
}
