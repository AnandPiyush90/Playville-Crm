package com.playville.crm.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The application uses Spring MVC without Boot's Jackson auto-configuration.
 * Provide the shared mapper needed to serialize onboarding draft snapshots.
 */
@Configuration
public class JacksonConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}
