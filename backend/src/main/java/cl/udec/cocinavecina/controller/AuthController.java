package cl.udec.cocinavecina.controller;

import cl.udec.cocinavecina.dto.Auth;
import cl.udec.cocinavecina.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public Auth.SignUpResponse register(@Valid @RequestBody Auth.SignUpRequest request) {
        return authService.register(request);
    }

    @GetMapping("/verify")
    public String verify(@RequestParam String token) {
        authService.verify(token);
        return "Cuenta verificada. Ya puedes volver a la app.";
    }

    @PostMapping("/login")
    public Auth.TokenResponse login(@Valid @RequestBody Auth.LoginRequest request) {
        return authService.login(request);
    }
}
