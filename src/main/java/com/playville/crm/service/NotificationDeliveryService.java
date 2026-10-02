package com.playville.crm.service;

import com.playville.crm.context.BranchContext;
import com.playville.crm.dto.notification.NotificationDeliveryResponse;
import com.playville.crm.entity.*;
import com.playville.crm.exception.*;
import com.playville.crm.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;

@Service @RequiredArgsConstructor
public class NotificationDeliveryService {
    private final NotificationDeliveryRepository deliveries; private final EmailOutboxRepository outbox; private final EmailCredentialCrypto crypto; private final EmailProviderConfigRepository providerConfigs;

    @Transactional public NotificationDelivery enqueueEmail(Branch branch, Customer customer, Staff staff, String purpose, String referenceType, String referenceId, String destination, String key, String subject, String body, byte[] attachment, String attachmentFilename) {
        Optional<NotificationDelivery> existing=deliveries.findByBranchIdAndIdempotencyKey(branch.getId(),key); if(existing.isPresent())return existing.get();
        EmailProviderConfig provider=providerConfigs.findByBranchId(branch.getId()).orElseThrow(()->new BusinessRuleException("EMAIL_CONFIGURATION_REQUIRED: Configure branch email provider settings"));
        if(!provider.isEnabled())throw new BusinessRuleException("EMAIL_SHARING_DISABLED: Enable the branch email provider");
        if(provider.getEncryptedPassword()==null||provider.getEncryptedPassword().isBlank())throw new BusinessRuleException("EMAIL_PASSWORD_REQUIRED: Configure and test the branch email provider before sending");
        if(attachment!=null&&attachment.length>10_000_000)throw new BusinessRuleException("EMAIL_ATTACHMENT_TOO_LARGE: Maximum attachment size is 10 MB");
        LocalDateTime now=LocalDateTime.now();
        NotificationDelivery delivery=deliveries.saveAndFlush(NotificationDelivery.builder().branch(branch).customer(customer).triggeredByStaff(staff).channel("EMAIL").purpose(purpose).referenceType(referenceType).referenceId(referenceId).providerType(provider.getProviderType()).destination(destination).status("PENDING").idempotencyKey(key).attemptCount(0).nextRetryAt(now).build());
        outbox.save(EmailOutbox.builder().delivery(delivery).encryptedSubject(crypto.encrypt(subject)).encryptedBody(crypto.encrypt(body)).attachmentBytes(attachment).attachmentFilename(attachmentFilename).nextAttemptAt(now).build());
        return delivery;
    }
    @Transactional(readOnly=true) public List<NotificationDeliveryResponse> currentBranchHistory(int page,int size) {
        return deliveries.findByBranchIdOrderByCreatedAtDesc(BranchContext.getBranchId(),PageRequest.of(Math.max(0,page),Math.min(100,Math.max(1,size)))).stream().map(this::response).toList();
    }
    @Transactional public NotificationDeliveryResponse retry(Integer id) {
        NotificationDelivery d=deliveries.findById(id).orElseThrow(()->new ResourceNotFoundException("Notification delivery",id)); if(!d.getBranch().getId().equals(BranchContext.getBranchId()))throw new BranchAccessDeniedException();
        if(!"EMAIL".equals(d.getChannel()))throw new BusinessRuleException("NOTIFICATION_RETRY_UNSUPPORTED: Only email deliveries can be retried");
        EmailOutbox o=outbox.findByDeliveryId(id).orElseThrow(()->new BusinessRuleException("NOTIFICATION_OUTBOX_MISSING")); LocalDateTime now=LocalDateTime.now(); o.setStatus("PENDING");o.setNextAttemptAt(now);o.setLockedAt(null);d.setStatus("PENDING");d.setAttemptCount(0);d.setNextRetryAt(now);d.setLastAttemptAt(null);d.setSentAt(null);d.setProviderReference(null);d.setErrorMessage(null); return response(d);
    }
    public NotificationDeliveryResponse response(NotificationDelivery d){return NotificationDeliveryResponse.builder().id(d.getId()).purpose(d.getPurpose()).channel(d.getChannel()).destination(d.getDestination()).status(d.getStatus()).referenceType(d.getReferenceType()).referenceId(d.getReferenceId()).attemptCount(d.getAttemptCount()).createdAt(d.getCreatedAt()).sentAt(d.getSentAt()).nextRetryAt(d.getNextRetryAt()).lastAttemptAt(d.getLastAttemptAt()).errorCode(d.getErrorMessage()).build();}
}
