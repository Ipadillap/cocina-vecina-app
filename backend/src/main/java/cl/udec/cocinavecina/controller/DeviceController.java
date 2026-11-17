package cl.udec.cocinavecina.controller;

import cl.udec.cocinavecina.components.CurrentUser;
import cl.udec.cocinavecina.dto.NewDevice;
import cl.udec.cocinavecina.service.DeviceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/devices")
public class DeviceController {
    private final DeviceService devices;

    public DeviceController(DeviceService devices) {
        this.devices = devices;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void register(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody NewDevice body) {
        devices.register(CurrentUser.id(jwt), body.pushToken());
    }

    @DeleteMapping("/{pushToken}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unregister(@AuthenticationPrincipal Jwt jwt, @PathVariable String pushToken) {
        devices.unregister(CurrentUser.id(jwt), pushToken);
    }
}
