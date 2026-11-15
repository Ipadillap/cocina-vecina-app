package cl.udec.cocinavecina.controller;

import cl.udec.cocinavecina.components.CurrentUser;
import cl.udec.cocinavecina.dto.NewPickUpPointRequest;
import cl.udec.cocinavecina.dto.PickUpPointView;
import cl.udec.cocinavecina.service.OfferService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/pickup-points")
public class PickUpPointController {
    private final OfferService offerService;

    public PickUpPointController(OfferService offerService) {
        this.offerService = offerService;
    }

    @GetMapping
    @PreAuthorize("hasRole('SELLER')")
    public List<PickUpPointView> mine(@AuthenticationPrincipal Jwt jwt) {
        return offerService.myPickUpPoints(CurrentUser.id(jwt));
    }

    @PostMapping
    @PreAuthorize("hasRole('SELLER')")
    @ResponseStatus(HttpStatus.CREATED)
    public PickUpPointView create(@AuthenticationPrincipal Jwt jwt,
                                  @Valid @RequestBody NewPickUpPointRequest request) {
        return offerService.createPickUpPoint(CurrentUser.id(jwt), request);
    }
}
