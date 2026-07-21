package com.playville.crm.controller;

import com.playville.crm.common.*;
import com.playville.crm.dto.audit.AuditLogResponse;
import com.playville.crm.service.AuditService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;

@RestController @RequestMapping("/audit-logs") @RequiredArgsConstructor
@PreAuthorize("hasAnyRole('admin','manager')")
public class AuditController {
    private final AuditService auditService;
    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<AuditLogResponse>>> search(@AuthenticationPrincipal String username,
            @RequestParam(required=false) LocalDateTime from, @RequestParam(required=false) LocalDateTime to,
            @RequestParam(required=false) String action, @RequestParam(required=false) String resource,
            @RequestParam(required=false) String outcome, @RequestParam(required=false) Integer actorStaffId,
            @RequestParam(required=false) Integer branchId, @RequestParam(required=false) String search,
            @RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="25") int size) {
        return ResponseEntity.ok(PagedResponse.of(auditService.search(username, from, to, action, resource, outcome, actorStaffId, branchId, search, page, size)));
    }
}
