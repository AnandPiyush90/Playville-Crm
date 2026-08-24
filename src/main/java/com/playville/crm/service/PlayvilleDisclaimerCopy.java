package com.playville.crm.service;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class PlayvilleDisclaimerCopy {
    public static final String TEMPLATE_CODE = "PLAYVILLE_CONDITIONS_OF_ENTRY";
    public static final String TITLE = "PlayVille Conditions of Entry";
    public static final String VERSION = "1.0";

    public String html() {
        try {
            return new ClassPathResource("disclaimer/conditions-of-entry.html")
                    .getContentAsString(StandardCharsets.UTF_8)
                    .trim();
        } catch (IOException exception) {
            throw new IllegalStateException("PlayVille disclaimer sample is missing from the classpath", exception);
        }
    }
}
