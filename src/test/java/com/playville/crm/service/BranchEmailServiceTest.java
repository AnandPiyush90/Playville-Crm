package com.playville.crm.service;

import com.playville.crm.context.BranchContext;
import com.playville.crm.dto.notification.EmailProviderConfigRequest;
import com.playville.crm.entity.Branch;
import com.playville.crm.entity.EmailProviderConfig;
import com.playville.crm.repository.BranchRepository;
import com.playville.crm.repository.EmailProviderConfigRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import java.util.Base64;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class BranchEmailServiceTest {
    private final EmailProviderConfigRepository configs=mock(EmailProviderConfigRepository.class);
    private final BranchRepository branches=mock(BranchRepository.class);
    private final EmailCredentialCrypto crypto=new EmailCredentialCrypto(Base64.getEncoder().encodeToString(new byte[32]));
    private final BranchEmailService service=new BranchEmailService(configs,branches,crypto);

    @AfterEach void clear(){ BranchContext.clear(); }

    @Test void gmailPresetUsesSafeGmailDefaultsAndEncryptsAppPassword() {
        BranchContext.set(7,"PV7"); Branch branch=Branch.builder().id(7).build();
        when(branches.findById(7)).thenReturn(Optional.of(branch)); when(configs.findByBranchId(7)).thenReturn(Optional.empty());
        when(configs.save(any())).thenAnswer(i->i.getArgument(0));
        EmailProviderConfigRequest request=new EmailProviderConfigRequest(); request.setProviderType("GMAIL_SMTP"); request.setUsername("owner@gmail.com"); request.setPassword("abcd efgh ijkl mnop"); request.setFromEmail("owner@gmail.com"); request.setFromName("PlayVille"); request.setEnabled(true);

        var response=service.saveCurrent(request);

        ArgumentCaptor<EmailProviderConfig> saved=ArgumentCaptor.forClass(EmailProviderConfig.class); verify(configs).save(saved.capture());
        assertThat(saved.getValue().getHost()).isEqualTo("smtp.gmail.com"); assertThat(saved.getValue().getPort()).isEqualTo(587); assertThat(saved.getValue().getTlsMode()).isEqualTo("STARTTLS");
        assertThat(saved.getValue().getEncryptedPassword()).doesNotContain("abcdefghijklmnop"); assertThat(crypto.decrypt(saved.getValue().getEncryptedPassword())).isEqualTo("abcdefghijklmnop");
        assertThat(response.isPasswordConfigured()).isTrue();
    }
}
