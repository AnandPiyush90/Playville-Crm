package com.playville.crm.service;

import com.playville.crm.entity.*;
import com.playville.crm.entity.SessionDeduction.DeductionSource;
import com.playville.crm.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AutoCloseService {

    private final CheckinRepository          checkinRepository;
    private final CheckinKidRepository       checkinKidRepository;
    private final CustomerRepository         customerRepository;
    private final SessionDeductionRepository deductionRepository;
    private final CronLogRepository          cronLogRepository;

    // Runs every day at 9 PM IST — configured in application.properties
    @Scheduled(cron = "${app.cron.auto-close}",
               zone  = "${app.cron.timezone}")
    @Transactional
    public void autoCloseActiveCheckins() {

        LocalDateTime now    = LocalDateTime.now();
        LocalDateTime cutoff = now.withHour(0).withMinute(0)
                                  .withSecond(0).withNano(0);

        List<Checkin> activeCheckins = checkinRepository
                .findActiveCheckinsOlderThan(cutoff);

        int closed    = 0;
        int deducted  = 0;
        StringBuilder errors = new StringBuilder();

        for (Checkin checkin : activeCheckins) {
            try {
                Customer customer = checkin.getCustomer();

                List<CheckinKid> undeducted = checkinKidRepository
                        .findByCheckinIdAndSessionUsedFalse(checkin.getId());

                for (CheckinKid ck : undeducted) {
                    int before = customer.getGlobalSessionBalance();

                    if (before > 0) {
                        customer.setGlobalSessionBalance(before - 1);
                        ck.setSessionUsed(true);
                        checkinKidRepository.save(ck);

                        SessionDeduction deduction = SessionDeduction.builder()
                                .customer(customer)
                                .checkin(checkin)
                                .kid(ck.getKid())
                                .branch(checkin.getBranch())
                                .deductedAt(now)
                                .deductionSource(DeductionSource.Auto_Close)
                                .balanceBefore(before)
                                .balanceAfter(customer.getGlobalSessionBalance())
                                .build();
                        deductionRepository.save(deduction);
                        deducted++;
                    }
                }

                customer.setTotalVisits(customer.getTotalVisits() + 1);
                customerRepository.save(customer);

                checkin.setStatus(Checkin.CheckinStatus.Auto_Closed);
                checkin.setCheckoutTime(now);
                checkin.setAutoClosed(true);
                checkin.setSessionsDeducted(undeducted.size());
                checkinRepository.save(checkin);
                closed++;

            } catch (Exception e) {
                log.error("Auto-close failed for checkin {}: {}",
                        checkin.getId(), e.getMessage());
                errors.append("Checkin ").append(checkin.getId())
                      .append(": ").append(e.getMessage()).append("\n");
            }
        }

        // Write cron log
        log.info("Auto-close complete: {} checkins closed, {} sessions deducted",
                closed, deducted);

        CronLog cronLog = CronLog.builder()
                .runAt(now)
                .runEndAt(LocalDateTime.now())
                .checkinsClosedCount(closed)
                .sessionsDeducted(deducted)
                .errorCount(errors.length() > 0 ? 1 : 0)
                .errors(errors.length() > 0 ? errors.toString() : null)
                .status(errors.length() > 0
                        ? CronLog.CronStatus.Partial
                        : CronLog.CronStatus.Success)
                .triggeredBy(CronLog.TriggerSource.Scheduler)
                .build();
        cronLogRepository.save(cronLog);
    }
}