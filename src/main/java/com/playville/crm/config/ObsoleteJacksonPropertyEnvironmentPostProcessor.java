package com.playville.crm.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.core.env.PropertySource;

public class ObsoleteJacksonPropertyEnvironmentPostProcessor implements EnvironmentPostProcessor {
    private static final String OBSOLETE_PROPERTY = "spring.jackson.serialization.write-dates-as-timestamps";
    private static final String OBSOLETE_PROPERTY_CANONICAL = canonicalize(OBSOLETE_PROPERTY);

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        System.clearProperty(OBSOLETE_PROPERTY);

        MutablePropertySources propertySources = environment.getPropertySources();
        List<String> propertySourceNames = new ArrayList<>();
        for (PropertySource<?> propertySource : propertySources) {
            propertySourceNames.add(propertySource.getName());
        }

        for (String propertySourceName : propertySourceNames) {
            PropertySource<?> propertySource = propertySources.get(propertySourceName);
            if (propertySource instanceof EnumerablePropertySource<?> enumerablePropertySource
                    && containsObsoleteProperty(enumerablePropertySource)) {
                propertySources.replace(propertySourceName, new FilteredPropertySource(enumerablePropertySource));
            }
        }
    }

    private static boolean containsObsoleteProperty(EnumerablePropertySource<?> propertySource) {
        return Arrays.stream(propertySource.getPropertyNames())
                .anyMatch(ObsoleteJacksonPropertyEnvironmentPostProcessor::isObsoleteProperty);
    }

    private static boolean isObsoleteProperty(String propertyName) {
        return canonicalize(propertyName).equals(OBSOLETE_PROPERTY_CANONICAL);
    }

    private static String canonicalize(String propertyName) {
        return propertyName.replaceAll("[^A-Za-z0-9]", "").toLowerCase();
    }

    private static final class FilteredPropertySource extends EnumerablePropertySource<EnumerablePropertySource<?>> {
        private FilteredPropertySource(EnumerablePropertySource<?> propertySource) {
            super(propertySource.getName(), propertySource);
        }

        @Override
        public String[] getPropertyNames() {
            return Arrays.stream(source.getPropertyNames())
                    .filter(propertyName -> !isObsoleteProperty(propertyName))
                    .toArray(String[]::new);
        }

        @Override
        public Object getProperty(String name) {
            if (isObsoleteProperty(name)) {
                return null;
            }
            return source.getProperty(name);
        }
    }
}
