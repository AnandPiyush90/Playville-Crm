package com.playville.crm.security;

import com.playville.crm.context.BranchContext;
import com.playville.crm.service.AuditService;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.util.*;

@Slf4j @Component @RequiredArgsConstructor
public class AuditCaptureFilter extends OncePerRequestFilter {
    private static final Set<String> MUTATIONS = Set.of("POST", "PUT", "PATCH", "DELETE");
    private final AuditService auditService;

    @Override protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !MUTATIONS.contains(request.getMethod()) || path.endsWith("/auth/login") || path.endsWith("/auth/hash");
    }

    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String correlationId = UUID.randomUUID().toString();
        response.setHeader("X-Correlation-ID", correlationId);
        Throwable failure = null;
        try { chain.doFilter(request, response); }
        catch (Throwable throwable) { failure = throwable; throw throwable; }
        finally { capture(request, response, failure, correlationId); }
    }

    private void capture(HttpServletRequest request, HttpServletResponse response, Throwable failure, String correlationId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getName())) return;
        try {
            String path = request.getRequestURI();
            String normalized = path.replaceFirst("^/api/v1/?", "");
            String[] segments = Arrays.stream(normalized.split("/")).filter(v -> !v.isBlank()).toArray(String[]::new);
            String resource = segments.length == 0 ? "SYSTEM" : segments[0].replace('-', '_').toUpperCase(Locale.ROOT);
            String resourceId = Arrays.stream(segments).filter(v -> v.matches("\\d+")).findFirst().orElse(null);
            String action = action(request.getMethod(), normalized);
            int status = failure == null ? response.getStatus() : 500;
            String outcome = status >= 400 ? "FAILURE" : "SUCCESS";
            Integer branchId = null;
            try { branchId = BranchContext.getBranchId(); } catch (Exception ignored) { }
            String role = auth.getAuthorities().stream().findFirst().map(a -> a.getAuthority().replace("ROLE_", "")).orElse(null);
            Map<String,Object> metadata = new LinkedHashMap<>();
            if (request.getHeader("X-Correlation-ID") != null) metadata.put("clientCorrelationId", truncate(request.getHeader("X-Correlation-ID"), 100));
            if (request.getQueryString() != null) metadata.put("query", request.getQueryString());
            if (failure != null) metadata.put("errorType", failure.getClass().getSimpleName());
            String description = human(action) + " " + human(resource) + (resourceId == null ? "" : " #" + resourceId)
                    + " - " + outcome.toLowerCase(Locale.ROOT);
            auditService.record(new AuditService.AuditCommand(branchId, auth.getName(), role, action, resource, resourceId,
                    outcome, description, request.getMethod(), path, status, clientIp(request), truncate(request.getHeader("User-Agent"), 500),
                    correlationId, metadataJson(metadata)));
        } catch (Exception exception) { log.error("Unable to persist audit event {}: {}", correlationId, exception.getMessage()); }
    }

    private String action(String method, String path) {
        String p = path.toLowerCase(Locale.ROOT);
        if (p.contains("/checkout")) return "CHECKOUT";
        if (p.contains("/finalize")) return "FINALIZE";
        if (p.contains("/payments")) return "PAYMENT";
        if (p.contains("/returns")) return "RETURN";
        if (p.contains("/share")) return "SHARE";
        if (p.contains("/cancel")) return "CANCEL";
        if (p.contains("/dispatch")) return "DISPATCH";
        if (p.contains("/receive")) return "RECEIVE";
        if (p.contains("/reactivate")) return "REACTIVATE";
        return switch (method) { case "POST" -> "CREATE"; case "DELETE" -> "DELETE"; default -> "UPDATE"; };
    }
    private String clientIp(HttpServletRequest request) { String forwarded = request.getHeader("X-Forwarded-For"); return forwarded == null ? request.getRemoteAddr() : forwarded.split(",")[0].trim(); }
    private String human(String value) { String lower = value.replace('_', ' ').toLowerCase(Locale.ROOT); return Character.toUpperCase(lower.charAt(0)) + lower.substring(1); }
    private String truncate(String value, int max) { return value == null || value.length() <= max ? value : value.substring(0, max); }
    private String metadataJson(Map<String,Object> metadata) {
        if (metadata.isEmpty()) return null;
        return metadata.entrySet().stream().map(entry -> "\"" + jsonEscape(entry.getKey()) + "\":\"" + jsonEscape(String.valueOf(entry.getValue())) + "\"")
                .collect(java.util.stream.Collectors.joining(",", "{", "}"));
    }
    private String jsonEscape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\r", "\\r").replace("\n", "\\n").replace("\t", "\\t");
    }
}
