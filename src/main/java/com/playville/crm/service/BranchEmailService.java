package com.playville.crm.service;

import com.playville.crm.context.BranchContext;
import com.playville.crm.dto.notification.*;
import com.playville.crm.entity.*;
import com.playville.crm.exception.*;
import com.playville.crm.repository.*;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Properties;

@Service @RequiredArgsConstructor
public class BranchEmailService {
    private final EmailProviderConfigRepository configs;
    private final BranchRepository branches;
    private final EmailCredentialCrypto crypto;

    @Transactional(readOnly=true) public EmailProviderConfigResponse getCurrent() {
        return response(configs.findByBranchId(BranchContext.getBranchId()).orElse(null));
    }
    @Transactional public EmailProviderConfigResponse saveCurrent(EmailProviderConfigRequest request) {
        Branch branch=branches.findById(BranchContext.getBranchId()).orElseThrow(()->new ResourceNotFoundException("Branch",BranchContext.getBranchId()));
        EmailProviderConfig config=configs.findByBranchId(branch.getId()).orElseGet(()->EmailProviderConfig.builder().branch(branch).build());
        boolean isNew=config.getId()==null;
        String type=request.getProviderType();
        config.setProviderType(type); config.setHost("GMAIL_SMTP".equals(type)?"smtp.gmail.com":required(request.getHost(),"EMAIL_HOST_REQUIRED"));
        config.setPort("GMAIL_SMTP".equals(type)?587:required(request.getPort(),"EMAIL_PORT_REQUIRED")); config.setTlsMode("GMAIL_SMTP".equals(type)?"STARTTLS":required(request.getTlsMode(),"EMAIL_TLS_MODE_REQUIRED"));
        if("GMAIL_SMTP".equals(type)&&!request.getUsername().matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$"))throw new BusinessRuleException("EMAIL_USERNAME_INVALID: Gmail username must be a complete email address");
        config.setUsername(request.getUsername().trim()); config.setFromEmail(request.getFromEmail().trim()); config.setFromName(request.getFromName().trim()); config.setReplyToEmail(blank(request.getReplyToEmail())); config.setEnabled(request.getEnabled());
        if(request.getPassword()!=null&&!request.getPassword().isBlank()) { String password="GMAIL_SMTP".equals(type)?request.getPassword().replace(" ",""):request.getPassword(); config.setEncryptedPassword(crypto.encrypt(password)); }
        if(isNew&&(config.getEncryptedPassword()==null||config.getEncryptedPassword().isBlank())) throw new BusinessRuleException("EMAIL_PASSWORD_REQUIRED: Enter an SMTP app password");
        if(config.isEnabled()&&(config.getEncryptedPassword()==null||config.getEncryptedPassword().isBlank())) throw new BusinessRuleException("EMAIL_PASSWORD_REQUIRED: Enter an SMTP app password before enabling email");
        return response(configs.save(config));
    }
    @Transactional(noRollbackFor = BusinessRuleException.class) public EmailProviderConfigResponse testCurrent(EmailTestRequest request) {
        EmailProviderConfig config=configs.findByBranchId(BranchContext.getBranchId()).orElseThrow(()->new BusinessRuleException("EMAIL_CONFIGURATION_REQUIRED: Save email provider settings first"));
        try { send(config,request.getDestination(),"PlayVille email configuration test","This confirms that your PlayVille email provider configuration is working.",null); config.setLastTestStatus("SUCCESS"); config.setLastErrorCode(null); }
        catch(Exception e) { config.setLastTestStatus("FAILED"); config.setLastErrorCode(errorCode(e)); throw new BusinessRuleException("EMAIL_TEST_FAILED: Could not send test email. Check provider settings and app password"); }
        finally { config.setLastTestedAt(LocalDateTime.now()); configs.save(config); }
        return response(config);
    }
    public String send(Integer branchId,String destination,String subject,String body,Attachment attachment) {
        EmailProviderConfig config=configs.findByBranchId(branchId).orElseThrow(()->new BusinessRuleException("EMAIL_CONFIGURATION_REQUIRED: Configure branch email provider settings"));
        if(!config.isEnabled()) throw new BusinessRuleException("EMAIL_SHARING_DISABLED: Enable the branch email provider");
        try { return send(config,destination,subject,body,attachment); }
        catch (BusinessRuleException e) { throw e; }
        catch (MailAuthenticationException e) { throw new BusinessRuleException("EMAIL_AUTHENTICATION_FAILED: Check the SMTP username and app password"); }
        catch (Exception e) { throw new IllegalStateException("Email provider delivery failed",e); }
    }
    private String send(EmailProviderConfig config,String destination,String subject,String body,Attachment attachment) throws Exception {
        JavaMailSenderImpl sender=new JavaMailSenderImpl(); sender.setHost(config.getHost()); sender.setPort(config.getPort()); sender.setUsername(config.getUsername()); sender.setPassword(crypto.decrypt(config.getEncryptedPassword())); sender.setProtocol("smtp");
        Properties p=sender.getJavaMailProperties(); p.put("mail.smtp.auth","true"); p.put("mail.smtp.connectiontimeout","10000"); p.put("mail.smtp.timeout","20000"); p.put("mail.smtp.writetimeout","20000");
        if("SSL_TLS".equals(config.getTlsMode())) p.put("mail.smtp.ssl.enable","true"); else p.put("mail.smtp.starttls.enable","true");
        MimeMessage message=sender.createMimeMessage(); MimeMessageHelper helper=new MimeMessageHelper(message,attachment!=null,StandardCharsets.UTF_8.name()); helper.setFrom(config.getFromEmail(),config.getFromName()); helper.setTo(destination); helper.setSubject(subject); helper.setText(body); if(config.getReplyToEmail()!=null)helper.setReplyTo(config.getReplyToEmail()); if(attachment!=null)helper.addAttachment(attachment.filename(),attachment.content()); sender.send(message); return message.getMessageID();
    }
    private String required(String value,String code){if(value==null||value.isBlank())throw new BusinessRuleException(code);return value.trim();}
    private int required(Integer value,String code){if(value==null)throw new BusinessRuleException(code);return value;}
    private String blank(String value){return value==null||value.isBlank()?null:value.trim();}
    private String errorCode(Exception e){String name=e.getClass().getSimpleName().replaceAll("[^A-Za-z0-9_]",""); return name.isBlank()?"EMAIL_PROVIDER_ERROR":name.substring(0,Math.min(name.length(),100));}
    private EmailProviderConfigResponse response(EmailProviderConfig c){ if(c==null)return null; return EmailProviderConfigResponse.builder().id(c.getId()).providerType(c.getProviderType()).host(c.getHost()).port(c.getPort()).username(c.getUsername()).tlsMode(c.getTlsMode()).fromEmail(c.getFromEmail()).fromName(c.getFromName()).replyToEmail(c.getReplyToEmail()).enabled(c.isEnabled()).passwordConfigured(c.getEncryptedPassword()!=null&&!c.getEncryptedPassword().isBlank()).lastTestStatus(c.getLastTestStatus()).lastTestedAt(c.getLastTestedAt()).lastErrorCode(c.getLastErrorCode()).build(); }
    public record Attachment(String filename, org.springframework.core.io.Resource content) {}
}
