package com.playville.crm.service;

import com.playville.crm.context.BranchContext;
import com.playville.crm.entity.*;
import com.playville.crm.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import java.time.LocalDateTime;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class NotificationDeliveryServiceTest {
    private final NotificationDeliveryRepository deliveries=mock(NotificationDeliveryRepository.class);
    private final EmailOutboxRepository outbox=mock(EmailOutboxRepository.class);
    private final EmailProviderConfigRepository providers=mock(EmailProviderConfigRepository.class);
    private final EmailCredentialCrypto crypto=new EmailCredentialCrypto(Base64.getEncoder().encodeToString(new byte[32]));
    private final NotificationDeliveryService service=new NotificationDeliveryService(deliveries,outbox,crypto,providers);
    @AfterEach void clear(){BranchContext.clear();}

    @Test void manualRetryResetsExhaustedDelivery() {
        BranchContext.set(7,"PV7"); NotificationDelivery delivery=NotificationDelivery.builder().id(9).branch(Branch.builder().id(7).build()).channel("EMAIL").status("FAILED").attemptCount(4).sentAt(LocalDateTime.now()).providerReference("old").build();
        EmailOutbox message=EmailOutbox.builder().delivery(delivery).status("FAILED").nextAttemptAt(LocalDateTime.now()).build(); when(deliveries.findById(9)).thenReturn(Optional.of(delivery)); when(outbox.findByDeliveryId(9)).thenReturn(Optional.of(message));

        service.retry(9);

        assertThat(delivery.getStatus()).isEqualTo("PENDING"); assertThat(delivery.getAttemptCount()).isZero(); assertThat(delivery.getProviderReference()).isNull(); assertThat(message.getStatus()).isEqualTo("PENDING");
    }
}
