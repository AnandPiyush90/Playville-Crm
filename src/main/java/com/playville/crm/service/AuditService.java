package com.playville.crm.service;

import com.playville.crm.context.BranchContext;
import com.playville.crm.dto.audit.AuditLogResponse;
import com.playville.crm.entity.*;
import com.playville.crm.entity.enums.StaffRole;
import com.playville.crm.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.time.LocalDateTime;

@Service @RequiredArgsConstructor
public class AuditService {
    private final AuditLogRepository auditRepository;
    private final StaffRepository staffRepository;
    private final BranchRepository branchRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(AuditCommand command) {
        Staff actor = command.username() == null ? null : staffRepository.findByUsernameAndIsActiveTrue(command.username()).orElse(null);
        Branch branch = command.branchId() == null ? null : branchRepository.findById(command.branchId()).orElse(null);
        auditRepository.save(AuditLog.builder().branch(branch).actorStaff(actor).actorUsername(command.username())
                .actorName(actor == null ? command.username() : actor.getFullName())
                .actorRole(actor == null || actor.getRole() == null ? command.role() : actor.getRole().name())
                .actionType(command.action()).resourceType(command.resource()).resourceId(command.resourceId())
                .outcome(command.outcome()).description(command.description()).httpMethod(command.httpMethod())
                .requestPath(command.path()).responseStatus(command.responseStatus()).ipAddress(command.ipAddress())
                .userAgent(command.userAgent()).correlationId(command.correlationId()).metadataJson(command.metadataJson())
                .occurredAt(LocalDateTime.now()).build());
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> search(String username, LocalDateTime from, LocalDateTime to, String action,
            String resource, String outcome, Integer actorStaffId, Integer requestedBranchId, String text, int page, int size) {
        Staff viewer = staffRepository.findByUsernameAndIsActiveTrue(username).orElseThrow();
        Integer branchId = viewer.getRole() == StaffRole.admin ? requestedBranchId : BranchContext.getBranchId();
        Specification<AuditLog> spec = (root, query, builder) -> builder.conjunction();
        if (branchId != null) spec = spec.and((r,q,b) -> b.equal(r.get("branch").get("id"), branchId));
        if (from != null) spec = spec.and((r,q,b) -> b.greaterThanOrEqualTo(r.get("occurredAt"), from));
        if (to != null) spec = spec.and((r,q,b) -> b.lessThan(r.get("occurredAt"), to));
        if (notBlank(action)) spec = spec.and((r,q,b) -> b.equal(r.get("actionType"), action.trim().toUpperCase()));
        if (notBlank(resource)) spec = spec.and((r,q,b) -> b.equal(r.get("resourceType"), resource.trim().toUpperCase()));
        if (notBlank(outcome)) spec = spec.and((r,q,b) -> b.equal(r.get("outcome"), outcome.trim().toUpperCase()));
        if (actorStaffId != null) spec = spec.and((r,q,b) -> b.equal(r.get("actorStaff").get("id"), actorStaffId));
        if (notBlank(text)) { String like = "%" + text.trim().toLowerCase() + "%"; spec = spec.and((r,q,b) -> b.or(
                b.like(b.lower(r.get("description")), like), b.like(b.lower(r.get("actorName")), like),
                b.like(b.lower(r.get("resourceId")), like), b.like(b.lower(r.get("correlationId")), like))); }
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 100), Sort.by(Sort.Direction.DESC, "occurredAt"));
        return auditRepository.findAll(spec, pageable).map(this::response);
    }

    private AuditLogResponse response(AuditLog a) { return AuditLogResponse.builder().id(a.getId())
            .branchId(a.getBranch() == null ? null : a.getBranch().getId()).branchCode(a.getBranch() == null ? null : a.getBranch().getBranchCode())
            .actorStaffId(a.getActorStaff() == null ? null : a.getActorStaff().getId()).actorUsername(a.getActorUsername())
            .actorName(a.getActorName()).actorRole(a.getActorRole()).actionType(a.getActionType()).resourceType(a.getResourceType())
            .resourceId(a.getResourceId()).outcome(a.getOutcome()).description(a.getDescription()).httpMethod(a.getHttpMethod())
            .requestPath(a.getRequestPath()).responseStatus(a.getResponseStatus()).ipAddress(a.getIpAddress())
            .correlationId(a.getCorrelationId()).metadataJson(a.getMetadataJson()).occurredAt(a.getOccurredAt()).build(); }
    private boolean notBlank(String value) { return value != null && !value.isBlank(); }

    public record AuditCommand(Integer branchId, String username, String role, String action, String resource,
            String resourceId, String outcome, String description, String httpMethod, String path,
            Integer responseStatus, String ipAddress, String userAgent, String correlationId, String metadataJson) { }
}
