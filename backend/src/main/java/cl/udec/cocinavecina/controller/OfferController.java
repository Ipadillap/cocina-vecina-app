package cl.udec.cocinavecina.controller;

import cl.udec.cocinavecina.components.CurrentUser;
import cl.udec.cocinavecina.dto.NearbyOfferView;
import cl.udec.cocinavecina.dto.NearbySort;
import cl.udec.cocinavecina.dto.NewOfferRequest;
import cl.udec.cocinavecina.dto.OfferView;
import cl.udec.cocinavecina.model.Allergen;
import cl.udec.cocinavecina.model.Category;
import cl.udec.cocinavecina.service.OfferService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1/offers")
public class OfferController {
    private final OfferService offerService;

    public OfferController(OfferService offerService) {
        this.offerService = offerService;
    }

    @PostMapping
    @PreAuthorize("hasRole('SELLER')")
    @ResponseStatus(HttpStatus.CREATED)
    public OfferView publish(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody NewOfferRequest request) {
        return offerService.publish(CurrentUser.id(jwt), request);
    }

    @GetMapping("/nearby")
    public List<NearbyOfferView> nearby(
        @RequestParam double lat,
        @RequestParam double lon,
        @RequestParam(defaultValue = "1500") int radius,
        @RequestParam(required = false) Category category,
        @RequestParam(required = false) List<Allergen> excludeAllergens,
        @RequestParam(defaultValue = "DISTANCE") NearbySort order
    ) {
        return offerService.nearby(lat, lon, radius, category, excludeAllergens, order);
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('SELLER')")
    public List<OfferView> mine(@AuthenticationPrincipal Jwt jwt) {
        return offerService.bySeller(CurrentUser.id(jwt));
    }

    @GetMapping("/{id}")
    public OfferView detail(@PathVariable Long id) {
        return offerService.detail(id);
    }

    @PatchMapping("/{id}/close")
    @PreAuthorize("hasRole('SELLER')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void close(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        offerService.close(CurrentUser.id(jwt), id);
    }
}
