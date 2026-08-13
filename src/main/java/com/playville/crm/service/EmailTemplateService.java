package com.playville.crm.service;

import com.playville.crm.context.BranchContext;
import com.playville.crm.dto.notification.*;
import com.playville.crm.entity.*;
import com.playville.crm.exception.*;
import com.playville.crm.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.regex.*;

@Service @RequiredArgsConstructor
public class EmailTemplateService {
    public static final String INVOICE="INVOICE";
    public static final String DISCLAIMER_SIGNING="DISCLAIMER_SIGNING";
    private static final Pattern VARIABLE=Pattern.compile("\\{\\{([A-Z][A-Z0-9_]*)}}",Pattern.CASE_INSENSITIVE);
    private final EmailTemplateRepository templates;
    private final BranchRepository branches;

    @Transactional(readOnly=true) public List<EmailTemplateResponse> listCurrent(){
        Integer branchId=BranchContext.getBranchId(); Map<String,EmailTemplate> saved=new HashMap<>(); templates.findByBranchId(branchId).forEach(t->saved.put(t.getTemplateKey(),t));
        return definitions().values().stream().map(d->response(d,saved.get(d.key()))).toList();
    }
    @Transactional(readOnly=true) public EmailTemplateResponse getCurrent(String key){Definition d=definition(key);return response(d,templates.findByBranchIdAndTemplateKey(BranchContext.getBranchId(),d.key()).orElse(null));}
    @Transactional public EmailTemplateResponse saveCurrent(String key,EmailTemplateRequest request){
        Definition d=definition(key); validateVariables(d,request.getSubject(),request.getBodyText()); Integer branchId=BranchContext.getBranchId();
        EmailTemplate t=templates.findByBranchIdAndTemplateKey(branchId,d.key()).orElseGet(()->EmailTemplate.builder().branch(branches.findById(branchId).orElseThrow(()->new ResourceNotFoundException("Branch",branchId))).templateKey(d.key()).build());
        if(t.getId()!=null&&(request.getVersion()==null||request.getVersion()!=t.getVersion()))throw new BusinessRuleException("EMAIL_TEMPLATE_CHANGED: Reload the template before saving your changes");
        t.setSubject(request.getSubject().trim());t.setBodyText(request.getBodyText().trim());return response(d,templates.save(t));
    }
    @Transactional public EmailTemplateResponse resetCurrent(String key){Definition d=definition(key);templates.deleteByBranchIdAndTemplateKey(BranchContext.getBranchId(),d.key());return response(d,null);}
    @Transactional(readOnly=true) public RenderedEmailTemplate previewCurrent(String key,EmailTemplatePreviewRequest request){
        Definition d=definition(key);EmailTemplate t=templates.findByBranchIdAndTemplateKey(BranchContext.getBranchId(),d.key()).orElse(null);Map<String,String> values=examples(d);if(request!=null&&request.getVariables()!=null)request.getVariables().forEach((k,v)->{if(d.variables().containsKey(k))values.put(k,v);});return renderTexts(t==null?d.defaultSubject():t.getSubject(),t==null?d.defaultBody():t.getBodyText(),values,d);
    }
    @Transactional(readOnly=true) public RenderedEmailTemplate render(Integer branchId,String key,Map<String,String> values){
        Definition d=definition(key);EmailTemplate t=templates.findByBranchIdAndTemplateKey(branchId,d.key()).orElse(null);return renderTexts(t==null?d.defaultSubject():t.getSubject(),t==null?d.defaultBody():t.getBodyText(),values,d);
    }
    private RenderedEmailTemplate renderTexts(String subject,String body,Map<String,String> values,Definition d){return new RenderedEmailTemplate(replace(subject,values,d),replace(body,values,d));}
    private String replace(String text,Map<String,String> values,Definition d){Matcher m=VARIABLE.matcher(text);StringBuffer out=new StringBuffer();while(m.find()){String key=m.group(1).toUpperCase(Locale.ROOT);if(!d.variables().containsKey(key))throw new BusinessRuleException("EMAIL_TEMPLATE_VARIABLE_INVALID: {{"+key+"}} is not available for "+d.name());String value=values==null?null:values.get(key);if(value==null)throw new BusinessRuleException("EMAIL_TEMPLATE_VALUE_MISSING: No value was supplied for {{"+key+"}}");m.appendReplacement(out,Matcher.quoteReplacement(value));}m.appendTail(out);return out.toString();}
    private void validateVariables(Definition d,String...texts){for(String text:texts){Matcher m=VARIABLE.matcher(text);while(m.find()){String key=m.group(1).toUpperCase(Locale.ROOT);if(!d.variables().containsKey(key))throw new BusinessRuleException("EMAIL_TEMPLATE_VARIABLE_INVALID: {{"+key+"}} is not available for "+d.name());}}}
    private Map<String,String> examples(Definition d){Map<String,String> values=new HashMap<>();d.variables().forEach((k,v)->values.put(k,v.example()));return values;}
    private Definition definition(String key){Definition d=definitions().get(key==null?"":key.toUpperCase(Locale.ROOT));if(d==null)throw new BusinessRuleException("EMAIL_TEMPLATE_NOT_SUPPORTED: Unknown email template");return d;}
    private EmailTemplateResponse response(Definition d,EmailTemplate t){return EmailTemplateResponse.builder().key(d.key()).name(d.name()).description(d.description()).subject(t==null?d.defaultSubject():t.getSubject()).bodyText(t==null?d.defaultBody():t.getBodyText()).variables(d.variables().entrySet().stream().map(e->new EmailTemplateResponse.Variable(e.getKey(),e.getValue().label(),e.getValue().example())).toList()).customized(t!=null).version(t==null?null:t.getVersion()).updatedAt(t==null?null:t.getUpdatedAt()).build();}
    private Map<String,Definition> definitions(){
        Map<String,VariableDefinition> common=new LinkedHashMap<>();common.put("BRANCH_NAME",new VariableDefinition("Branch name","PlayVille Whitefield"));
        Map<String,VariableDefinition> invoice=new LinkedHashMap<>(common);invoice.put("CUSTOMER_NAME",new VariableDefinition("Customer name","Asha Rao"));invoice.put("INVOICE_NUMBER",new VariableDefinition("Invoice number","PV7/1042"));
        Map<String,VariableDefinition> disclaimer=new LinkedHashMap<>(common);disclaimer.put("GUARDIAN_NAME",new VariableDefinition("Guardian name","Asha Rao"));disclaimer.put("DISCLAIMER_URL",new VariableDefinition("Secure signing link","https://example.com/sign/secure-token"));disclaimer.put("EXPIRES_AT",new VariableDefinition("Link expiry time","8 Aug 2026, 11:30 AM"));
        Map<String,Definition> all=new LinkedHashMap<>();all.put(INVOICE,new Definition(INVOICE,"Invoice Email","Sent with a finalized invoice PDF.","Your PlayVille invoice {{INVOICE_NUMBER}}","Hello {{CUSTOMER_NAME}},\n\nPlease find your PlayVille invoice {{INVOICE_NUMBER}} attached.\n\nThank you,\n{{BRANCH_NAME}}",invoice));all.put(DISCLAIMER_SIGNING,new Definition(DISCLAIMER_SIGNING,"Disclaimer Signing","Invites a guardian to review and accept the active disclaimer.","Please review and accept the PlayVille disclaimer","Hello {{GUARDIAN_NAME}},\n\nPlease review and accept the disclaimer using this secure link:\n{{DISCLAIMER_URL}}\n\nThis link expires at {{EXPIRES_AT}} and can be used once.\n\nThank you,\n{{BRANCH_NAME}}",disclaimer));return all;
    }
    private record Definition(String key,String name,String description,String defaultSubject,String defaultBody,Map<String,VariableDefinition> variables){}
    private record VariableDefinition(String label,String example){}
}
