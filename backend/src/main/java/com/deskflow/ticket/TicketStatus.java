package com.deskflow.ticket;

import java.util.Set;

public enum TicketStatus {
    OPEN,
    IN_PROGRESS,
    RESOLVED,
    CLOSED;

    private Set<TicketStatus> nextStatuses() {
        return switch (this) {
            case OPEN -> Set.of(IN_PROGRESS, CLOSED);
            case IN_PROGRESS -> Set.of(OPEN, RESOLVED);
            case RESOLVED -> Set.of(IN_PROGRESS, CLOSED);
            case CLOSED -> Set.of();
        };
    }

    public boolean canMoveTo(TicketStatus next) {
        return nextStatuses().contains(next);
    }

    public boolean isActive() {
        return this == OPEN || this == IN_PROGRESS;
    }
}
