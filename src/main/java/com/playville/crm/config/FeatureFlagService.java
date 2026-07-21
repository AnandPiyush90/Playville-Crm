package com.playville.crm.config;

import com.playville.crm.exception.BusinessRuleException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class FeatureFlagService {
    private final boolean trialConversionFlow;

    public FeatureFlagService(@Value("${app.features.trial-conversion-flow:true}") boolean trialConversionFlow) {
        this.trialConversionFlow = trialConversionFlow;
    }

    public void requireTrialConversionFlow() {
        if (!trialConversionFlow) {
            throw new BusinessRuleException("FEATURE_DISABLED: Trial conversion flow is disabled");
        }
    }
}
