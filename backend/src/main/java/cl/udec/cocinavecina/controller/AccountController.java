package cl.udec.cocinavecina.controller;

import cl.udec.cocinavecina.components.CurrentUser;
import cl.udec.cocinavecina.dto.AccountExport;
import cl.udec.cocinavecina.dto.AccountView;
import cl.udec.cocinavecina.dto.ConsentChange;
import cl.udec.cocinavecina.dto.ConsentView;
import cl.udec.cocinavecina.model.ConsentType;
import cl.udec.cocinavecina.service.AppUserService;
import cl.udec.cocinavecina.service.ConsentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/account")
public class AccountController {
    private final AppUserService appUserService;
    private final ConsentService consentService;

    public AccountController(AppUserService appUserService, ConsentService consentService) {
        this.appUserService = appUserService;
        this.consentService = consentService;
    }

    @GetMapping
    public AccountView account(@AuthenticationPrincipal Jwt jwt) {
        return appUserService.account(CurrentUser.id(jwt));
    }

    @GetMapping("/export")
    public AccountExport export(@AuthenticationPrincipal Jwt jwt) {
        return appUserService.export(CurrentUser.id(jwt));
    }

    @GetMapping("/consents")
    public List<ConsentView> consents(@AuthenticationPrincipal Jwt jwt) {
        return consentService.list(CurrentUser.id(jwt));
    }

    @PutMapping("/consents/{type}")
    public ConsentView setConsent(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable ConsentType type,
        @Valid @RequestBody ConsentChange body
    ) {
        return consentService.set(CurrentUser.id(jwt), type, body.granted());
    }

    @DeleteMapping
    public ResponseEntity<Void> delete(@AuthenticationPrincipal Jwt jwt) {
        final boolean deleted = appUserService.delete(CurrentUser.id(jwt));
        return deleted ? ResponseEntity.noContent().build() : ResponseEntity.accepted().build();
    }
}
