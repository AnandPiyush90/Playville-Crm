package com.playville.crm.service;

import com.playville.crm.entity.*;
import com.playville.crm.repository.EmailOutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service @RequiredArgsConstructor
public class EmailOutboxDispatcher {
    private final EmailOutboxRepository outbox; private final BranchEmailService email; private final EmailCredentialCrypto crypto;
    @Scheduled(fixedDelayString="${app.notifications.email-dispatch-delay-ms:15000}") @Transactional
    public void dispatch(){ processDue(); }
    @Transactional public void processDue(){
        for(EmailOutbox o:outbox.findDue(LocalDateTime.now(),PageRequest.of(0,20))) process(o);
    }
    private void process(EmailOutbox o){
        NotificationDelivery d=o.getDelivery(); LocalDateTime now=LocalDateTime.now(); o.setStatus("PROCESSING");o.setLockedAt(now);d.setLastAttemptAt(now);d.setAttemptCount(d.getAttemptCount()+1);
        try { String ref=email.send(d.getBranch().getId(),d.getDestination(),crypto.decrypt(o.getEncryptedSubject()),crypto.decrypt(o.getEncryptedBody()),o.getAttachmentBytes()==null?null:new BranchEmailService.Attachment(o.getAttachmentFilename(),new ByteArrayResource(o.getAttachmentBytes()))); d.setStatus("SENT");d.setProviderReference(ref);d.setSentAt(now);d.setNextRetryAt(null);d.setErrorMessage(null);o.setStatus("SENT"); }
        catch(Exception e){ boolean retry=!(e instanceof com.playville.crm.exception.BusinessRuleException)&&d.getAttemptCount()<4; String code=e.getClass().getSimpleName(); d.setErrorMessage(code); if(retry){LocalDateTime next=now.plusMinutes(d.getAttemptCount()==1?1:d.getAttemptCount()==2?5:30);d.setStatus("PENDING");d.setNextRetryAt(next);o.setStatus("PENDING");o.setNextAttemptAt(next);o.setLockedAt(null);}else{d.setStatus("FAILED");d.setNextRetryAt(null);o.setStatus("FAILED");} }
    }
}
