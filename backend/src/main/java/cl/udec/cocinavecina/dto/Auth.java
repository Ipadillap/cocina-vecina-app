package cl.udec.cocinavecina.dto;

import cl.udec.cocinavecina.model.Role;
import jakarta.validation.constraints.*;

public final class Auth {
    private Auth() {
    }

    public static final String EMAIL_REGEX = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";

    public static final String PASSWORD_REGEX =
        "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{10,128}$";

    public static final String CHILE_MOBILE_REGEX = "^(\\+?56)?\\s?9\\s?\\d{4}\\s?\\d{4}$";

    public record SignUpRequest(
        @NotBlank(message = "El correo electrónico está vacío.")
        @Email(regexp = EMAIL_REGEX, message = "Formato del correo electrónico no es correcto.")
        @Size(max = 254, message = "El correo electrónico es demasiado largo.")
        String email,
        @NotBlank(message = "El nombre de usuario está vacío.")
        @Size(max = 60, message = "El nombre de usuario no puede superar 60 caracteres.")
        String displayName,
        @NotBlank(message = "La contraseña no puede estar vacía.")
        @Pattern(
            regexp = PASSWORD_REGEX,
            message = "El formato de la contraseña no es correcto: entre 10 y 128 caracteres, sin espacios, con "
                + "mayúscula, minúscula, número y uno de @#$%^&+=!"
        )
        String password,
        @Pattern(
            regexp = CHILE_MOBILE_REGEX,
            message = "El formato del celular no es el adecuado, por ejemplo +56 9 1234 5678."
        )
        String phone,
        @NotNull(message = "Debe indicar si la cuenta es de comprador o vendedor.")
        Role role,
        @AssertTrue(message = "Debe declarar que es mayor de 18 años.")
        boolean adult,
        @AssertTrue(message = "Debe aceptar los términos y condiciones.")
        boolean acceptsTerms,
        boolean acceptsLocation,
        boolean acceptsNotifications
    ) {
    }

    public record SignUpResponse(
        long userId,
        String message
    ) {
    }

    public record LoginRequest(
        @NotBlank
        @Email
        String email,
        @NotBlank
        String password
    ) {
    }

    public record TokenResponse(
        String accessToken,
        long expiresInSeconds,
        long userId,
        Role role,
        boolean verified,
        String refreshToken
    ) {
    }

    public record RefreshRequest(
        @NotBlank
        String refreshToken
    ) {
    }
}
