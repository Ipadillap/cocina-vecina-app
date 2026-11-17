package cl.udec.cocinavecina.components;

import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Objects;

public final class CurrentUser {
    private CurrentUser() {
    }

    public static Long id(Jwt jwt) {
        return Long.valueOf(Objects.requireNonNull(jwt.getSubject()));
    }
}
