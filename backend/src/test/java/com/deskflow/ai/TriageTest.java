package com.deskflow.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.deskflow.ticket.TicketCategory;
import com.deskflow.ticket.TicketPriority;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

class TriageTest {

    private static final String LOCAL_URL = "http://localhost:11434/v1";

    private final RuleBasedTriageClient rules = new RuleBasedTriageClient();
    private final LlmTriageClient llm = mock(LlmTriageClient.class);

    private AiProperties properties(boolean enabled) {
        return new AiProperties(enabled, LOCAL_URL, "test-model", "", Duration.ofSeconds(1));
    }

    private static String completionWith(String content) {
        return JsonMapper.builder().build().writeValueAsString(java.util.Map.of(
                "choices", java.util.List.of(java.util.Map.of("message", java.util.Map.of("content", content)))));
    }

    @Test
    void rulesRecogniseBillingAndUrgency() {
        TriageResult result = rules.triage("Refund needed", "Production down since the payment failed");

        assertThat(result.category()).isEqualTo(TicketCategory.BILLING);
        assertThat(result.priority()).isEqualTo(TicketPriority.URGENT);
        assertThat(result.byAi()).isFalse();
    }

    @Test
    void rulesFallBackToGeneralAndMedium() {
        TriageResult result = rules.triage("Hello", "Thanks for the great product");

        assertThat(result.category()).isEqualTo(TicketCategory.GENERAL);
        assertThat(result.priority()).isEqualTo(TicketPriority.MEDIUM);
    }

    @Test
    void usesRulesWhenAiIsDisabled() {
        TriageResult result = new TriageService(properties(false), llm, rules).triage("Hello", "Hi");

        assertThat(result.byAi()).isFalse();
        verifyNoInteractions(llm);
    }

    @Test
    void fallsBackToRulesWhenTheModelFails() {
        when(llm.triage(any(), any())).thenThrow(new IllegalStateException("model offline"));

        TriageResult result = new TriageService(properties(true), llm, rules).triage("Refund", "Please refund me");

        assertThat(result.byAi()).isFalse();
        assertThat(result.category()).isEqualTo(TicketCategory.BILLING);
    }

    @Test
    void parsesAValidModelReply() {
        LlmTriageClient client = new LlmTriageClient(properties(true), JsonMapper.builder().build());

        TriageResult result = client.parse(completionWith(
                "{\"category\":\"TECHNICAL\",\"priority\":\"HIGH\",\"note\":\"App crash\"}"));

        assertThat(result.category()).isEqualTo(TicketCategory.TECHNICAL);
        assertThat(result.priority()).isEqualTo(TicketPriority.HIGH);
        assertThat(result.byAi()).isTrue();
    }

    @Test
    void rejectsAValueOutsideTheAllowedList() {
        LlmTriageClient client = new LlmTriageClient(properties(true), JsonMapper.builder().build());

        assertThatThrownBy(() -> client.parse(completionWith(
                "{\"category\":\"HACKED\",\"priority\":\"HIGH\",\"note\":\"x\"}")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
