package cl.udec.cocinavecina.dto;

import cl.udec.cocinavecina.model.Role;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class Auth {
    private Auth() {}

    public static final String PASSWORD_REGEX =
        "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{10,128}$";

    public static final String CHILE_MOBILE_REGEX = "^(\\+?56)?\\s?9\\s?\\d{4}\\s?\\d{4}$";

    public record SignUpRequest(@NotBlank @Email @Size(max = 254) String email,
                                @NotBlank @Size(max = 60) String displayName,
                                @NotBlank
                                @Pattern(regexp = PASSWORD_REGEX,
                                    message = "debe tener entre 10 y 128 caracteres, sin espacios, con mayúscula, minúscula, número y uno de @#$%^&+=!")
                                String password,
                                @NotBlank
                                @Pattern(regexp = CHILE_MOBILE_REGEX,
                                    message = "debe ser un celular chileno, por ejemplo +56 9 1234 5678")
                                String phone,
                                @NotNull Role role,
                                @AssertTrue(message = "debe declarar que es mayor de 18 años") boolean adult,
                                @AssertTrue(message = "debe aceptar los términos y condiciones") boolean acceptsTerms
    ) {}

    public record SignUpResponse(long userId,
                                 String message
    ) {}

    public record LoginRequest(@NotBlank @Email String email,
                               @NotBlank String password
    ) {}

    public record TokenResponse(String accessToken,
                                long expiresInSeconds,
                                long userId,
                                Role role,
                                boolean verified
    ) {}
}
