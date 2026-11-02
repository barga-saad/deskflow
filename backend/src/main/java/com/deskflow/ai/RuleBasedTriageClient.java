package com.deskflow.ai;

import com.deskflow.ticket.TicketCategory;
import com.deskflow.ticket.TicketPriority;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Keyword rules. They always work, so ticket creation never depends on an AI server. */
@Component
public class RuleBasedTriageClient implements TriageClient {

    private static final Map<TicketCategory, List<String>> CATEGORY_KEYWORDS = Map.of(
            TicketCategory.BILLING, List.of("invoice", "refund", "payment", "charged", "subscription", "billing"),
            TicketCategory.TECHNICAL, List.of("error", "crash", "bug", "broken", "timeout", "fails", "not working"),
            TicketCategory.ACCOUNT, List.of("password", "login", "log in", "sign in", "account", "locked"));

    private static final List<String> URGENT_KEYWORDS = List.of("outage", "production down", "data loss", "security breach");
    private static final List<String> HIGH_KEYWORDS = List.of("urgent", "asap", "cannot", "can't", "blocked", "crash");
    private static final List<String> LOW_KEYWORDS = List.of("question", "how do i", "feature request", "suggestion");

    @Override
    public TriageResult triage(String title, String description) {
        String text = (title + " " + description).toLowerCase(Locale.ROOT);
        return new TriageResult(categoryFor(text), priorityFor(text), "Classified by keyword rules", false);
    }

    private TicketCategory categoryFor(String text) {
        return CATEGORY_KEYWORDS.entrySet().stream()
                .map(entry -> Map.entry(entry.getKey(), countMatches(text, entry.getValue())))
                .filter(entry -> entry.getValue() > 0)
                .max(Comparator.comparingLong(Map.Entry::getValue))
                .map(Map.Entry::getKey)
                .orElse(TicketCategory.GENERAL);
    }

    private TicketPriority priorityFor(String text) {
        if (countMatches(text, URGENT_KEYWORDS) > 0) {
            return TicketPriority.URGENT;
        }
        if (countMatches(text, HIGH_KEYWORDS) > 0) {
            return TicketPriority.HIGH;
        }
        return countMatches(text, LOW_KEYWORDS) > 0 ? TicketPriority.LOW : TicketPriority.MEDIUM;
    }

    private long countMatches(String text, List<String> keywords) {
        return keywords.stream().filter(text::contains).count();
    }
}
