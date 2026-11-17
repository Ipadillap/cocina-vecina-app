package cl.udec.cocinavecina.controller;

import cl.udec.cocinavecina.components.CurrentUser;
import cl.udec.cocinavecina.dto.NewPurchaseRequest;
import cl.udec.cocinavecina.dto.Price;
import cl.udec.cocinavecina.dto.PurchaseRequestView;
import cl.udec.cocinavecina.service.NegotiationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/v1")
public class NegotiationController {
    private final NegotiationService negotiationService;

    public NegotiationController(NegotiationService negotiationService) {
        this.negotiationService = negotiationService;
    }

    @PostMapping("/offers/{offerId}/purchase-requests")
    @PreAuthorize("hasRole('BUYER')")
    @ResponseStatus(HttpStatus.CREATED)
    public PurchaseRequestView submit(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable Long offerId,
        @Valid @RequestBody NewPurchaseRequest body
    ) {
        return negotiationService.submit(CurrentUser.id(jwt), offerId, body);
    }

    @GetMapping("/purchase-requests/pending")
    @PreAuthorize("hasRole('SELLER')")
    public List<PurchaseRequestView> pending(@AuthenticationPrincipal Jwt jwt) {
        return negotiationService.pending(CurrentUser.id(jwt));
    }

    @GetMapping("/purchase-requests/mine")
    public List<PurchaseRequestView> mine(@AuthenticationPrincipal Jwt jwt) {
        return negotiationService.mine(CurrentUser.id(jwt));
    }

    @GetMapping("/purchase-requests/{id}")
    public PurchaseRequestView view(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return negotiationService.view(CurrentUser.id(jwt), id);
    }

    @PostMapping("/purchase-requests/{id}/counteroffers")
    @PreAuthorize("hasRole('SELLER')")
    public PurchaseRequestView counteroffer(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable Long id,
        @Valid @RequestBody Price body
    ) {
        return negotiationService.counteroffer(CurrentUser.id(jwt), id, body.unitPrice());
    }

    @PostMapping("/purchase-requests/{id}/proposals")
    @PreAuthorize("hasRole('BUYER')")
    public PurchaseRequestView propose(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable Long id,
        @Valid @RequestBody Price body
    ) {
        return negotiationService.propose(CurrentUser.id(jwt), id, body.unitPrice());
    }

    @PostMapping("/purchase-requests/{id}/accept")
    public PurchaseRequestView accept(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return negotiationService.accept(CurrentUser.id(jwt), id);
    }

    @PostMapping("/purchase-requests/{id}/reject")
    public PurchaseRequestView reject(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return negotiationService.reject(CurrentUser.id(jwt), id);
    }

    @PostMapping("/purchase-requests/{id}/withdraw")
    @PreAuthorize("hasRole('BUYER')")
    public PurchaseRequestView withdraw(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return negotiationService.withdraw(CurrentUser.id(jwt), id);
    }
}
