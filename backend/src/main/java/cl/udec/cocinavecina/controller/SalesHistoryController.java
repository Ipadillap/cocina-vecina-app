package cl.udec.cocinavecina.controller;

import cl.udec.cocinavecina.components.CurrentUser;
import cl.udec.cocinavecina.dto.SalesHistory;
import cl.udec.cocinavecina.exception.ApiException;
import cl.udec.cocinavecina.service.SalesHistoryService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;
import java.time.format.DateTimeParseException;

@RestController
@RequestMapping("/v1/seller")
public class SalesHistoryController {
    private final SalesHistoryService history;

    public SalesHistoryController(SalesHistoryService history) {
        this.history = history;
    }

    @GetMapping("/history")
    @PreAuthorize("hasRole('SELLER')")
    public SalesHistory history(@AuthenticationPrincipal Jwt jwt, @RequestParam(required = false) String month) {
        final YearMonth period;
        try {
            period = month == null ? SalesHistoryService.currentMonth() : YearMonth.parse(month);
        } catch (DateTimeParseException e) {
            throw ApiException.badRequest("month: el formato es yyyy-MM, por ejemplo 2026-11");
        }
        return history.month(CurrentUser.id(jwt), period);
    }
}
