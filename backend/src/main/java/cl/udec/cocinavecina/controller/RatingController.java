package cl.udec.cocinavecina.controller;

import cl.udec.cocinavecina.components.CurrentUser;
import cl.udec.cocinavecina.dto.NewRating;
import cl.udec.cocinavecina.dto.RatingSummary;
import cl.udec.cocinavecina.dto.RatingView;
import cl.udec.cocinavecina.service.RatingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1")
public class RatingController {
    private final RatingService ratingService;

    public RatingController(RatingService ratingService) {
        this.ratingService = ratingService;
    }

    @PostMapping("/orders/{id}/rating")
    @ResponseStatus(HttpStatus.CREATED)
    public RatingView rate(
        @AuthenticationPrincipal Jwt jwt,
        @PathVariable Long id,
        @Valid @RequestBody NewRating body
    ) {
        return ratingService.rate(CurrentUser.id(jwt), id, body.score());
    }

    @GetMapping("/users/{id}/rating")
    public RatingSummary summary(@PathVariable Long id) {
        return ratingService.summary(id);
    }
}
