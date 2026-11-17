package cl.udec.cocinavecina.controller;

import cl.udec.cocinavecina.components.CurrentUser;
import cl.udec.cocinavecina.dto.AccountExport;
import cl.udec.cocinavecina.dto.AccountView;
import cl.udec.cocinavecina.service.AppUserService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/account")
public class AccountController {
    private final AppUserService appUserService;

    public AccountController(AppUserService appUserService) {
        this.appUserService = appUserService;
    }

    @GetMapping
    public AccountView account(@AuthenticationPrincipal Jwt jwt) {
        return appUserService.account(CurrentUser.id(jwt));
    }

    @GetMapping("/export")
    public AccountExport export(@AuthenticationPrincipal Jwt jwt) {
        return appUserService.export(CurrentUser.id(jwt));
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Jwt jwt) {
        appUserService.delete(CurrentUser.id(jwt));
    }
}
