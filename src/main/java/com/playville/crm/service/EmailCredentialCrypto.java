package com.playville.crm.service;

import com.playville.crm.exception.BusinessRuleException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class EmailCredentialCrypto {
    private final String configuredKey;
    public EmailCredentialCrypto(@Value("${PLAYVILLE_EMAIL_ENCRYPTION_KEY:${app.email.encryption-key:}}") String configuredKey) { this.configuredKey = configuredKey; }
    public String encrypt(String value) {
        try {
            byte[] iv=new byte[12]; new SecureRandom().nextBytes(iv); Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key(), new GCMParameterSpec(128,iv)); byte[] encrypted=cipher.doFinal(value.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(iv)+":"+Base64.getEncoder().encodeToString(encrypted);
        } catch (BusinessRuleException ex) { throw ex; } catch (Exception ex) { throw new IllegalStateException("Unable to protect email credential",ex); }
    }
    public String decrypt(String value) {
        try {
            String[] parts=value.split(":",2); if(parts.length!=2) throw new IllegalArgumentException(); Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE,key(),new GCMParameterSpec(128,Base64.getDecoder().decode(parts[0])));
            return new String(cipher.doFinal(Base64.getDecoder().decode(parts[1])),java.nio.charset.StandardCharsets.UTF_8);
        } catch (BusinessRuleException ex) { throw ex; } catch (Exception ex) { throw new BusinessRuleException("EMAIL_CONFIGURATION_INVALID: Stored email credential cannot be read"); }
    }
    private SecretKeySpec key() {
        try { byte[] bytes=Base64.getDecoder().decode(configuredKey); if(bytes.length!=32) throw new IllegalArgumentException(); return new SecretKeySpec(bytes,"AES"); }
        catch (Exception ex) { throw new BusinessRuleException("EMAIL_ENCRYPTION_KEY_REQUIRED: Configure PLAYVILLE_EMAIL_ENCRYPTION_KEY as a 32-byte Base64 secret"); }
    }
}
