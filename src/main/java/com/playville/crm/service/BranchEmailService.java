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
        if(request.getPassword()!=null&&!request.getPassword().isBlank()) {
            String password="GMAIL_SMTP".equals(type)?gmailAppPassword(request.getPassword()):request.getPassword();
            if(password.isBlank()) throw new BusinessRuleException("EMAIL_PASSWORD_REQUIRED: Enter an SMTP app password");
            config.setEncryptedPassword(crypto.encrypt(password));
            config.setLastTestStatus(null); config.setLastErrorCode(null); config.setLastErrorMessage(null); config.setLastTestedAt(null);
        }
        if(isNew&&(config.getEncryptedPassword()==null||config.getEncryptedPassword().isBlank())) throw new BusinessRuleException("EMAIL_PASSWORD_REQUIRED: Enter an SMTP app password");
        if(config.isEnabled()&&(config.getEncryptedPassword()==null||config.getEncryptedPassword().isBlank())) throw new BusinessRuleException("EMAIL_PASSWORD_REQUIRED: Enter an SMTP app password before enabling email");
        return response(configs.save(config));
    }
    @Transactional(noRollbackFor = BusinessRuleException.class) public EmailProviderConfigResponse testCurrent(EmailTestRequest request) {
        EmailProviderConfig config=configs.findByBranchId(BranchContext.getBranchId()).orElseThrow(()->new BusinessRuleException("EMAIL_CONFIGURATION_REQUIRED: Save email provider settings first"));
        try {
            send(config,request.getDestination(),"PlayVille email configuration test","This confirms that your PlayVille email provider configuration is working.",null);
            config.setLastTestStatus("SUCCESS"); config.setLastErrorCode(null); config.setLastErrorMessage(null);
        } catch(Exception e) {
            config.setLastTestStatus("FAILED");
            String secretLen="GMAIL_SMTP".equals(config.getProviderType())?String.valueOf(gmailAppPassword(crypto.decrypt(config.getEncryptedPassword())).length()):"?";
            String detail=rootMessage(e);
            if(isAuthFailure(e)) {
                config.setLastErrorCode("GMAIL_AUTH_FAILED");
                config.setLastErrorMessage("Gmail rejected SMTP login for "+config.getUsername()+" using a "+secretLen+"-character secret. "+detail+" Copy the 16-letter App Password from Google (not your Gmail password), Save, then test again.");
            } else {
                config.setLastErrorCode(errorCode(e));
                config.setLastErrorMessage("Could not send test email ("+errorCode(e)+"). "+detail);
            }
        } finally {
            config.setLastTestedAt(LocalDateTime.now()); configs.save(config);
        }
        return response(config);
    }
    public String send(Integer branchId,String destination,String subject,String body,Attachment attachment) {
        EmailProviderConfig config=configs.findByBranchId(branchId).orElseThrow(()->new BusinessRuleException("EMAIL_CONFIGURATION_REQUIRED: Configure branch email provider settings"));
        if(!config.isEnabled()) throw new BusinessRuleException("EMAIL_SHARING_DISABLED: Enable the branch email provider");
        try { return send(config,destination,subject,body,attachment); }
        catch (BusinessRuleException e) { throw e; }
        catch (Exception e) {
            if(isAuthFailure(e)) throw new BusinessRuleException("EMAIL_AUTHENTICATION_FAILED: Check the SMTP username and app password");
            throw new IllegalStateException("Email provider delivery failed",e);
        }
    }
    private String send(EmailProviderConfig config,String destination,String subject,String body,Attachment attachment) throws Exception {
        String user=config.getUsername().trim();
        String password=crypto.decrypt(config.getEncryptedPassword());
        if("GMAIL_SMTP".equals(config.getProviderType())) { user=user.toLowerCase(); password=gmailAppPassword(password); }
        JavaMailSenderImpl sender=new JavaMailSenderImpl();
        sender.setHost(config.getHost()); sender.setPort(config.getPort()); sender.setUsername(user); sender.setPassword(password); sender.setProtocol("smtp"); sender.setDefaultEncoding(StandardCharsets.UTF_8.name());
        Properties props=sender.getJavaMailProperties();
        props.put("mail.smtp.auth","true"); props.put("mail.smtp.connectiontimeout","15000"); props.put("mail.smtp.timeout","20000"); props.put("mail.smtp.writetimeout","20000");
        if("SSL_TLS".equals(config.getTlsMode()) || config.getPort()==465) { props.put("mail.smtp.ssl.enable","true"); props.put("mail.smtp.ssl.trust", config.getHost()); }
        else { props.put("mail.smtp.starttls.enable","true"); props.put("mail.smtp.starttls.required","true"); props.put("mail.smtp.ssl.trust", config.getHost()); }
        MimeMessage message=sender.createMimeMessage();
        MimeMessageHelper helper=new MimeMessageHelper(message, attachment!=null, StandardCharsets.UTF_8.name());
        helper.setFrom(config.getFromEmail(), config.getFromName()); helper.setTo(destination); helper.setSubject(subject); helper.setText(body, false);
        if(config.getReplyToEmail()!=null) helper.setReplyTo(config.getReplyToEmail());
        if(attachment!=null) helper.addAttachment(attachment.filename(), attachment.content());
        sender.send(message);
        return message.getMessageID();
    }
    private boolean isAuthFailure(Throwable error) {
        while(error!=null) {
            if(error instanceof MailAuthenticationException || error instanceof jakarta.mail.AuthenticationFailedException) return true;
            String message=error.getMessage()==null?"":error.getMessage().toLowerCase();
            if(message.contains("username and password not accepted") || message.contains("535-5.7.8") || message.contains("535 5.7.8")) return true;
            error=error.getCause();
        }
        return false;
    }
    private String rootMessage(Throwable error) {
        Throwable current=error; String message=error.getClass().getSimpleName();
        while(current!=null) { if(current.getMessage()!=null&&!current.getMessage().isBlank()) message=current.getMessage(); current=current.getCause(); }
        return safeMessage(message);
    }
    private String safeMessage(String message) {
        String value=message==null?"":message.replaceAll("(?i)password[^\\s]{0,40}", "password");
        return value.length()>220?value.substring(0,220):value;
    }
    private String gmailAppPassword(String value) {
        return value==null?"":value.replaceAll("[\\s\\u00A0\\u200B\\uFEFF]+","");
    }
    private String required(String value,String code){if(value==null||value.isBlank())throw new BusinessRuleException(code);return value.trim();}
    private int required(Integer value,String code){if(value==null)throw new BusinessRuleException(code);return value;}
    private String blank(String value){return value==null||value.isBlank()?null:value.trim();}
    private String errorCode(Exception e){String name=e.getClass().getSimpleName().replaceAll("[^A-Za-z0-9_]",""); return name.isBlank()?"EMAIL_PROVIDER_ERROR":name.substring(0,Math.min(name.length(),100));}
    private EmailProviderConfigResponse response(EmailProviderConfig c){ if(c==null)return null; return EmailProviderConfigResponse.builder().id(c.getId()).providerType(c.getProviderType()).host(c.getHost()).port(c.getPort()).username(c.getUsername()).tlsMode(c.getTlsMode()).fromEmail(c.getFromEmail()).fromName(c.getFromName()).replyToEmail(c.getReplyToEmail()).enabled(c.isEnabled()).passwordConfigured(c.getEncryptedPassword()!=null&&!c.getEncryptedPassword().isBlank()).lastTestStatus(c.getLastTestStatus()).lastTestedAt(c.getLastTestedAt()).lastErrorCode(c.getLastErrorCode()).lastErrorMessage(c.getLastErrorMessage()).build(); }
    public record Attachment(String filename, org.springframework.core.io.Resource content) {}
}
