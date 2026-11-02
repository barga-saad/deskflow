package com.deskflow.ticket;

import java.time.Duration;

public enum TicketPriority {
    LOW(Duration.ofHours(72)),
    MEDIUM(Duration.ofHours(24)),
    HIGH(Duration.ofHours(8)),
    URGENT(Duration.ofHours(4));

    private final Duration responseTarget;

    TicketPriority(Duration responseTarget) {
        this.responseTarget = responseTarget;
    }

    public Duration responseTarget() {
        return responseTarget;
    }
}
