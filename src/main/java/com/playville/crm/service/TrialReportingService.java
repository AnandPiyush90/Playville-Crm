package com.playville.crm.service;

import com.playville.crm.context.BranchContext;
import com.playville.crm.config.FeatureFlagService;
import com.playville.crm.dto.report.TrialFunnelResponse;
import com.playville.crm.dto.report.TrialConversionResponse;
import com.playville.crm.exception.BranchAccessDeniedException;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.*;
import java.time.*;
import java.sql.Timestamp;
import java.util.List;

@Service @RequiredArgsConstructor
public class TrialReportingService {
    private final EntityManager entityManager;
    private final FeatureFlagService featureFlags;

    @Transactional(readOnly = true)
    public TrialFunnelResponse funnel(LocalDate from, LocalDate to, Integer branchId, Integer staffId, String leadSource, String campaignCode) {
        featureFlags.requireTrialConversionFlow();
        LocalDate start = from == null ? LocalDate.now().minusDays(30) : from;
        LocalDate end = to == null ? LocalDate.now() : to;
        if (end.isBefore(start)) throw new IllegalArgumentException("to must not be before from");
        Integer scopedBranchId = scopedBranch(branchId);
        String fromSql = reportingFromSql();
        long issued = count("select count(distinct e.id) " + fromSql, scopedBranchId, staffId, start, end.plusDays(1), leadSource, campaignCode, "");
        long checkedIn = count("select count(distinct ci.entitlement_id) " + fromSql, scopedBranchId, staffId, start, end.plusDays(1), leadSource, campaignCode, " and ci.id is not null");
        long completed = count("select count(distinct ci.entitlement_id) " + fromSql, scopedBranchId, staffId, start, end.plusDays(1), leadSource, campaignCode, " and ci.status = 'Completed'");
        long sameDay = count("select count(distinct p.id) " + fromSql, scopedBranchId, staffId, start, end.plusDays(1), leadSource, campaignCode, " and date(p.created_at) = date(e.created_at)");
        long sevenDays = count("select count(distinct p.id) " + fromSql, scopedBranchId, staffId, start, end.plusDays(1), leadSource, campaignCode, " and p.created_at >= e.created_at and p.created_at < date_add(e.created_at, interval 8 day)");
        long expired = count("select count(distinct e.id) " + fromSql, scopedBranchId, staffId, start, end.plusDays(1), leadSource, campaignCode, " and e.status = 'EXPIRED'");
        long declined = count("select count(distinct ci.id) " + fromSql, scopedBranchId, staffId, start, end.plusDays(1), leadSource, campaignCode, " and ci.conversion_outcome = 'DECLINED'");
        BigDecimal rate = issued == 0 ? BigDecimal.ZERO : BigDecimal.valueOf(sevenDays).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(issued), 2, RoundingMode.HALF_UP);
        return TrialFunnelResponse.builder().branchId(scopedBranchId).from(start).to(end).issued(issued).checkedIn(checkedIn).completed(completed).purchasedSameDay(sameDay).purchasedWithinSevenDays(sevenDays).expiredOrNoShow(expired).declined(declined).conversionRate(rate).build();
    }

    @Transactional(readOnly = true)
    public Page<TrialConversionResponse> conversions(LocalDate from, LocalDate to, Integer branchId, Integer staffId,
                                                     String leadSource, String campaignCode, int page, int size) {
        featureFlags.requireTrialConversionFlow();
        LocalDate start = from == null ? LocalDate.now().minusDays(30) : from;
        LocalDate end = to == null ? LocalDate.now() : to;
        Integer scopedBranchId = scopedBranch(branchId);
        String filters = """
                e.branch_id = :branchId
                and e.type = 'COMPLIMENTARY_TRIAL'
                and e.created_at >= :from
                and e.created_at < :to
                and (:staffId is null or ci.staff_id = :staffId)
                and (:campaignCode is null or e.campaign_code = :campaignCode)
                and (:leadSource is null or c.lead_source = :leadSource)
                """;
        String fromSql = """
                from pv_customer_entitlements e
                join pv_customers c on c.id = e.customer_id
                left join pv_checkins ci on ci.entitlement_id = e.id
                left join pv_purchases p on p.source_trial_entitlement_id = e.id
                where
                """ + filters;

        @SuppressWarnings("unchecked")
        List<Object[]> rows = entityManager.createNativeQuery("""
                select e.id, ci.id, c.id, c.parent_name, c.phone_number, e.campaign_code, c.lead_source,
                       ci.conversion_outcome, ci.conversion_reason, p.id, p.amount_paid,
                       e.created_at, ci.checkin_time, ci.checkout_time, ci.follow_up_at
                """ + fromSql + " order by e.created_at desc limit :limit offset :offset")
                .setParameter("branchId", scopedBranchId)
                .setParameter("from", start.atStartOfDay())
                .setParameter("to", end.plusDays(1).atStartOfDay())
                .setParameter("staffId", staffId)
                .setParameter("leadSource", blankToNull(leadSource))
                .setParameter("campaignCode", blankToNull(campaignCode))
                .setParameter("limit", size)
                .setParameter("offset", page * size)
                .getResultList();

        Number total = (Number) entityManager.createNativeQuery("select count(*) " + fromSql)
                .setParameter("branchId", scopedBranchId)
                .setParameter("from", start.atStartOfDay())
                .setParameter("to", end.plusDays(1).atStartOfDay())
                .setParameter("staffId", staffId)
                .setParameter("leadSource", blankToNull(leadSource))
                .setParameter("campaignCode", blankToNull(campaignCode))
                .getSingleResult();

        List<TrialConversionResponse> content = rows.stream().map(this::conversionRow).toList();
        return new PageImpl<>(content, PageRequest.of(page, size), total.longValue());
    }

    private long count(String sql, Integer branchId, Integer staffId, LocalDate from, LocalDate to, String source, String campaign, String extraFilter) {
        Number result = (Number) entityManager.createNativeQuery(sql + extraFilter)
                .setParameter("branchId", branchId)
                .setParameter("from", from.atStartOfDay())
                .setParameter("to", to.atStartOfDay())
                .setParameter("staffId", staffId)
                .setParameter("leadSource", blankToNull(source))
                .setParameter("campaignCode", blankToNull(campaign))
                .getSingleResult();
        return result.longValue();
    }
    private String reportingFromSql() {
        return """
                from pv_customer_entitlements e
                join pv_customers c on c.id = e.customer_id
                left join pv_checkins ci on ci.entitlement_id = e.id
                left join pv_purchases p on p.source_trial_entitlement_id = e.id
                where e.branch_id = :branchId
                and e.type = 'COMPLIMENTARY_TRIAL'
                and e.created_at >= :from
                and e.created_at < :to
                and (:staffId is null or ci.staff_id = :staffId)
                and (:campaignCode is null or e.campaign_code = :campaignCode)
                and (:leadSource is null or c.lead_source = :leadSource)
                """;
    }
    private Integer scopedBranch(Integer requestedBranchId) { Integer current = BranchContext.getBranchId(); if (requestedBranchId != null && !requestedBranchId.equals(current)) throw new BranchAccessDeniedException(); return current; }
    private String blankToNull(String value) { return value == null || value.isBlank() ? null : value; }
    private TrialConversionResponse conversionRow(Object[] row) {
        return TrialConversionResponse.builder()
                .entitlementId(intValue(row[0]))
                .checkinId(intValue(row[1]))
                .customerId(intValue(row[2]))
                .parentName((String) row[3])
                .phoneNumber((String) row[4])
                .campaignCode((String) row[5])
                .leadSource((String) row[6])
                .conversionOutcome((String) row[7])
                .conversionReason((String) row[8])
                .purchaseId(intValue(row[9]))
                .amountPaid((BigDecimal) row[10])
                .trialIssuedAt(timeValue(row[11]))
                .checkinTime(timeValue(row[12]))
                .checkoutTime(timeValue(row[13]))
                .followUpAt(timeValue(row[14]))
                .build();
    }
    private Integer intValue(Object value) { return value == null ? null : ((Number) value).intValue(); }
    private LocalDateTime timeValue(Object value) {
        if (value == null) return null;
        if (value instanceof LocalDateTime localDateTime) return localDateTime;
        return ((Timestamp) value).toLocalDateTime();
    }
}
