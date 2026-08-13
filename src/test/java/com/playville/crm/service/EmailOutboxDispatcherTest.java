package com.playville.crm.service;

import com.playville.crm.entity.*;
import com.playville.crm.repository.EmailOutboxRepository;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class EmailOutboxDispatcherTest {
    private final EmailOutboxRepository outbox=mock(EmailOutboxRepository.class);
    private final BranchEmailService email=mock(BranchEmailService.class);
    private final EmailCredentialCrypto crypto=new EmailCredentialCrypto(Base64.getEncoder().encodeToString(new byte[32]));
    private final EmailOutboxDispatcher dispatcher=new EmailOutboxDispatcher(outbox,email,crypto);

    @Test void dispatchMarksDeliverySent() {
        NotificationDelivery delivery=NotificationDelivery.builder().id(8).branch(Branch.builder().id(7).build()).destination("guardian@example.com").status("PENDING").build();
        EmailOutbox message=EmailOutbox.builder().delivery(delivery).encryptedSubject(crypto.encrypt("Subject")).encryptedBody(crypto.encrypt("Body")).nextAttemptAt(java.time.LocalDateTime.now()).build();
        when(outbox.findDue(any(),any())).thenReturn(List.of(message)); when(email.send(eq(7),eq("guardian@example.com"),eq("Subject"),eq("Body"),isNull())).thenReturn("provider-id");

        dispatcher.processDue();

        assertThat(delivery.getStatus()).isEqualTo("SENT"); assertThat(delivery.getAttemptCount()).isEqualTo(1); assertThat(message.getStatus()).isEqualTo("SENT");
    }

    @Test void failureIsQueuedForRetry() {
        NotificationDelivery delivery=NotificationDelivery.builder().id(8).branch(Branch.builder().id(7).build()).destination("guardian@example.com").status("PENDING").build();
        EmailOutbox message=EmailOutbox.builder().delivery(delivery).encryptedSubject(crypto.encrypt("Subject")).encryptedBody(crypto.encrypt("Body")).nextAttemptAt(java.time.LocalDateTime.now()).build();
        when(outbox.findDue(any(),any())).thenReturn(List.of(message)); when(email.send(any(),any(),any(),any(),any())).thenThrow(new IllegalStateException("provider unavailable"));

        dispatcher.processDue();

        assertThat(delivery.getStatus()).isEqualTo("PENDING"); assertThat(delivery.getNextRetryAt()).isNotNull(); assertThat(message.getStatus()).isEqualTo("PENDING");
    }

    @Test void permanentConfigurationFailureIsNotRetried() {
        NotificationDelivery delivery=NotificationDelivery.builder().id(8).branch(Branch.builder().id(7).build()).destination("guardian@example.com").status("PENDING").build();
        EmailOutbox message=EmailOutbox.builder().delivery(delivery).encryptedSubject(crypto.encrypt("Subject")).encryptedBody(crypto.encrypt("Body")).nextAttemptAt(java.time.LocalDateTime.now()).build();
        when(outbox.findDue(any(),any())).thenReturn(List.of(message)); when(email.send(any(),any(),any(),any(),any())).thenThrow(new com.playville.crm.exception.BusinessRuleException("EMAIL_AUTHENTICATION_FAILED"));

        dispatcher.processDue();

        assertThat(delivery.getStatus()).isEqualTo("FAILED"); assertThat(delivery.getNextRetryAt()).isNull(); assertThat(message.getStatus()).isEqualTo("FAILED");
    }

    @Test void thirdTransientFailureSchedulesThirtyMinuteRetry() {
        NotificationDelivery delivery=NotificationDelivery.builder().id(8).branch(Branch.builder().id(7).build()).destination("guardian@example.com").status("PENDING").attemptCount(2).build();
        EmailOutbox message=EmailOutbox.builder().delivery(delivery).encryptedSubject(crypto.encrypt("Subject")).encryptedBody(crypto.encrypt("Body")).nextAttemptAt(java.time.LocalDateTime.now()).build();
        when(outbox.findDue(any(),any())).thenReturn(List.of(message)); when(email.send(any(),any(),any(),any(),any())).thenThrow(new IllegalStateException("timeout"));

        dispatcher.processDue();

        assertThat(delivery.getStatus()).isEqualTo("PENDING"); assertThat(delivery.getAttemptCount()).isEqualTo(3); assertThat(delivery.getNextRetryAt()).isAfter(java.time.LocalDateTime.now().plusMinutes(29));
    }
}
