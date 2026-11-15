package cl.udec.cocinavecina.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class SearchRateLimitFilter extends OncePerRequestFilter {

    private record Window(long minute, AtomicInteger count) {}

    private final int limitPerMinute;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    public SearchRateLimitFilter(int limitPerMinute) {
        this.limitPerMinute = limitPerMinute;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/v1/offers/nearby");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String key = auth != null ? auth.getName() : request.getRemoteAddr();
        long minute = System.currentTimeMillis() / 60_000;
        if (windows.size() > 10_000) {
            windows.values().removeIf(w -> w.minute() != minute);
        }
        Window window = windows.compute(key, (k, v) ->
            v == null || v.minute() != minute ? new Window(minute, new AtomicInteger()) : v);
        if (window.count().incrementAndGet() > limitPerMinute) {
            response.setStatus(429);
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"title\":\"Demasiadas búsquedas\",\"status\":429,"
                + "\"detail\":[\"Se superó el límite de búsquedas por minuto (RNF-S-5)\"]}");
            return;
        }
        chain.doFilter(request, response);
    }
}
