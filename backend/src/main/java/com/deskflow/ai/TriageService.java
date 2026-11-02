package com.deskflow.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Uses the AI model when it is enabled, and falls back to the rules on any failure. */
@Service
public class TriageService {

    private static final Logger log = LoggerFactory.getLogger(TriageService.class);

    private final AiProperties properties;
    private final LlmTriageClient llmClient;
    private final RuleBasedTriageClient ruleBasedClient;

    public TriageService(AiProperties properties, LlmTriageClient llmClient, RuleBasedTriageClient ruleBasedClient) {
        this.properties = properties;
        this.llmClient = llmClient;
        this.ruleBasedClient = ruleBasedClient;
    }

    public TriageResult triage(String title, String description) {
        if (properties.enabled()) {
            try {
                return llmClient.triage(title, description);
            } catch (RuntimeException ex) {
                log.warn("AI triage failed, using keyword rules instead: {}", ex.getMessage());
            }
        }
        return ruleBasedClient.triage(title, description);
    }
}
