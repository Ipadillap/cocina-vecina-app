package cl.udec.cocinavecina.exception;

import cl.udec.cocinavecina.dto.ApiResponse;
import cl.udec.cocinavecina.dto.HTTP;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ApiException.class)
    ResponseEntity<ApiResponse> api(ApiException e) {
        ApiResponse response = e.getResponse();
        List<String> detail = response.detail() != null
            ? response.detail()
            : e.getMessage() == null ? List.of() : List.of(e.getMessage());
        return ResponseEntity.status(response.status())
            .body(new ApiResponse(response.title(), response.status(), detail));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiResponse> invalidBody(MethodArgumentNotValidException e) {
        List<String> detail = e.getBindingResult().getFieldErrors().stream()
            .map(f -> f.getField() + ": " + f.getDefaultMessage())
            .toList();
        return body(HTTP.Response.UNPROCESSABLE_ENTITY, "Datos inválidos", detail);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ApiResponse> unreadableBody(HttpMessageNotReadableException e) {
        return body(HTTP.Response.BAD_REQUEST, "Solicitud inválida",
            List.of("El cuerpo no es un JSON válido o tiene un valor no permitido"));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    ResponseEntity<ApiResponse> missingParameter(MissingServletRequestParameterException e) {
        return body(HTTP.Response.BAD_REQUEST, "Solicitud inválida",
            List.of(e.getParameterName() + ": es obligatorio"));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ApiResponse> invalidParameter(MethodArgumentTypeMismatchException e) {
        return body(HTTP.Response.BAD_REQUEST, "Solicitud inválida",
            List.of(e.getName() + ": valor no permitido"));
    }

    private static ResponseEntity<ApiResponse> body(HTTP.Response status, String title, List<String> detail) {
        return ResponseEntity.status(status.status()).body(new ApiResponse(title, status.status(), detail));
    }
}
