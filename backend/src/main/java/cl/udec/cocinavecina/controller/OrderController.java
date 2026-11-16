package cl.udec.cocinavecina.controller;

import cl.udec.cocinavecina.components.CurrentUser;
import cl.udec.cocinavecina.dto.Cancellation;
import cl.udec.cocinavecina.dto.CodeValidation;
import cl.udec.cocinavecina.dto.CodeView;
import cl.udec.cocinavecina.dto.OrderView;
import cl.udec.cocinavecina.dto.PickupDetailsView;
import cl.udec.cocinavecina.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/orders")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public List<OrderView> mine(@AuthenticationPrincipal Jwt jwt) {
        return orderService.mine(CurrentUser.id(jwt));
    }

    @GetMapping("/{id}")
    public OrderView view(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return orderService.view(CurrentUser.id(jwt), id);
    }

    @GetMapping("/{id}/pickup")
    public PickupDetailsView pickup(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return orderService.pickup(CurrentUser.id(jwt), id);
    }

    @PostMapping("/{id}/code")
    public CodeView issueCode(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return orderService.issueCode(CurrentUser.id(jwt), id);
    }

    @PatchMapping("/{id}/ready")
    public OrderView ready(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return orderService.markReady(CurrentUser.id(jwt), id);
    }

    @PostMapping("/{id}/delivery")
    public OrderView delivery(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable Long id,
        @Valid @RequestBody CodeValidation body
    ) {
        return orderService.validateDelivery(CurrentUser.id(jwt), id, body.code());
    }

    @PostMapping("/{id}/cancellation")
    public OrderView cancel(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable Long id,
        @Valid @RequestBody Cancellation body
    ) {
        return orderService.cancel(CurrentUser.id(jwt), id, body.reason());
    }
}
