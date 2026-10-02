package com.playville.crm.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.playville.crm.common.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class ApiRateLimitFilter extends OncePerRequestFilter {
    private final ObjectMapper objectMapper;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    @Value("${app.api.rate-limit.max-requests:60}")
    private int maxRequests;

    @Value("${app.api.rate-limit.window-seconds:60}")
    private long windowSeconds;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if ("OPTIONS".equals(request.getMethod())) return true;
        String path = request.getRequestURI();
        return path.endsWith("/error") || path.endsWith("/auth/login");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        long now = System.currentTimeMillis();
        long windowMillis = Math.max(1, windowSeconds) * 1000L;
        Window window = windows.compute(rateLimitKey(request), (ignored, current) -> {
            if (current == null || now - current.startedAt >= windowMillis) return new Window(now);
            current.requests++;
            return current;
        });

        if (window.requests > Math.max(1, maxRequests)) {
            long retryAfter = Math.max(1, (window.startedAt + windowMillis - now + 999) / 1000);
            response.setStatus(429);
            response.setHeader("Retry-After", String.valueOf(retryAfter));
            response.setContentType("application/json");
            response.getWriter().write(objectMapper.writeValueAsString(ApiResponse.failure(
                    "Too many API requests. Retry after " + retryAfter + " seconds.")));
            return;
        }
        chain.doFilter(request, response);
    }

    private String rateLimitKey(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !"anonymousUser".equals(authentication.getName())) {
            return "user:" + authentication.getName();
        }
        return "ip:" + request.getRemoteAddr();
    }

    private static final class Window {
        private final long startedAt;
        private int requests = 1;

        private Window(long startedAt) {
            this.startedAt = startedAt;
        }
    }
}