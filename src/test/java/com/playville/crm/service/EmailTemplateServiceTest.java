package com.playville.crm.service;

import com.playville.crm.context.BranchContext;
import com.playville.crm.dto.notification.*;
import com.playville.crm.entity.*;
import com.playville.crm.exception.BusinessRuleException;
import com.playville.crm.repository.*;
import org.junit.jupiter.api.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class EmailTemplateServiceTest {
    private final EmailTemplateRepository templates=mock(EmailTemplateRepository.class);
    private final BranchRepository branches=mock(BranchRepository.class);
    private final EmailTemplateService service=new EmailTemplateService(templates,branches);
    @BeforeEach void context(){BranchContext.set(7,"PV7");}
    @AfterEach void clear(){BranchContext.clear();}

    @Test void returnsUsableDefaultsBeforeCustomization(){
        when(templates.findByBranchId(7)).thenReturn(List.of());
        var result=service.listCurrent();
        assertThat(result).extracting(EmailTemplateResponse::getKey).containsExactly("INVOICE","DISCLAIMER_SIGNING");
        assertThat(result).allMatch(t->!t.isCustomized()&&!t.getVariables().isEmpty());
    }

    @Test void rendersConfiguredInvoiceTemplate(){
        EmailTemplate saved=EmailTemplate.builder().templateKey("INVOICE").subject("Invoice {{INVOICE_NUMBER}}").bodyText("Hi {{CUSTOMER_NAME}} from {{BRANCH_NAME}}").build();
        when(templates.findByBranchIdAndTemplateKey(7,"INVOICE")).thenReturn(Optional.of(saved));
        var rendered=service.render(7,"INVOICE",Map.of("INVOICE_NUMBER","PV7/42","CUSTOMER_NAME","Asha","BRANCH_NAME","PlayVille"));
        assertThat(rendered.subject()).isEqualTo("Invoice PV7/42");assertThat(rendered.bodyText()).isEqualTo("Hi Asha from PlayVille");
    }

    @Test void rejectsUnsupportedVariablesBeforeSaving(){
        EmailTemplateRequest request=new EmailTemplateRequest();request.setSubject("Hi {{PASSWORD}}");request.setBodyText("Body");
        assertThatThrownBy(()->service.saveCurrent("INVOICE",request)).isInstanceOf(BusinessRuleException.class).hasMessageContaining("PASSWORD");
        verifyNoInteractions(branches);
    }

    @Test void resetRemovesOverrideAndReturnsDefault(){
        var result=service.resetCurrent("INVOICE");
        verify(templates).deleteByBranchIdAndTemplateKey(7,"INVOICE");assertThat(result.isCustomized()).isFalse();assertThat(result.getSubject()).contains("{{INVOICE_NUMBER}}");
    }

    @Test void preventsBlindOverwriteOfAnotherAdminsChanges(){
        EmailTemplate saved=EmailTemplate.builder().id(4L).branch(Branch.builder().id(7).build()).templateKey("INVOICE").subject("Old").bodyText("Old body").version(3).build();when(templates.findByBranchIdAndTemplateKey(7,"INVOICE")).thenReturn(Optional.of(saved));
        EmailTemplateRequest request=new EmailTemplateRequest();request.setSubject("New");request.setBodyText("New body");request.setVersion(2L);
        assertThatThrownBy(()->service.saveCurrent("INVOICE",request)).isInstanceOf(BusinessRuleException.class).hasMessageContaining("Reload");
        verify(templates,never()).save(any());
    }
}
