package com.deskflow.ai;

import com.deskflow.ticket.TicketCategory;
import com.deskflow.ticket.TicketPriority;

public record TriageResult(TicketCategory category, TicketPriority priority, String note, boolean byAi) {
}
