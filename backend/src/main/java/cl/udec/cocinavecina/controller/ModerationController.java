package cl.udec.cocinavecina.controller;

import cl.udec.cocinavecina.components.CurrentUser;
import cl.udec.cocinavecina.dto.AccountDecision;
import cl.udec.cocinavecina.dto.NewReport;
import cl.udec.cocinavecina.dto.OfferDecision;
import cl.udec.cocinavecina.dto.OfferUnderReview;
import cl.udec.cocinavecina.service.ModerationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ModerationController {
    private final ModerationService moderation;

    public ModerationController(ModerationService moderation) {
        this.moderation = moderation;
    }

    @PostMapping("/v1/offers/{id}/reports")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void report(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id, @Valid @RequestBody NewReport body) {
        moderation.report(CurrentUser.id(jwt), id, body);
    }

    @GetMapping("/v1/moderation/hidden-offers")
    @PreAuthorize("hasRole('MODERATOR')")
    public List<OfferUnderReview> hiddenOffers() {
        return moderation.hiddenOffers();
    }

    @PatchMapping("/v1/moderation/offers/{id}")
    @PreAuthorize("hasRole('MODERATOR')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void decideOffer(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable Long id,
        @Valid @RequestBody OfferDecision body
    ) {
        moderation.decideOffer(CurrentUser.id(jwt), id, body);
    }

    @PatchMapping("/v1/moderation/accounts/{id}")
    @PreAuthorize("hasRole('MODERATOR')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void decideAccount(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable Long id,
        @Valid @RequestBody AccountDecision body
    ) {
        moderation.decideAccount(CurrentUser.id(jwt), id, body);
    }
}
