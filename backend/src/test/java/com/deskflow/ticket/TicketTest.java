package com.deskflow.ticket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.deskflow.common.ConflictException;
import com.deskflow.user.Role;
import com.deskflow.user.User;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class TicketTest {

    private Ticket newTicket(TicketPriority priority) {
        User requester = new User("a@b.test", "A", "hash", Role.CUSTOMER);
        return new Ticket("Title", "Description", requester, priority, TicketCategory.GENERAL, null, false);
    }

    @ParameterizedTest
    @CsvSource({
            "OPEN, IN_PROGRESS, true",
            "OPEN, RESOLVED, false",
            "IN_PROGRESS, RESOLVED, true",
            "RESOLVED, IN_PROGRESS, true",
            "RESOLVED, CLOSED, true",
            "CLOSED, OPEN, false"
    })
    void statusTransitionsFollowTheWorkflow(TicketStatus from, TicketStatus to, boolean allowed) {
        assertThat(from.canMoveTo(to)).isEqualTo(allowed);
    }

    @Test
    void rejectsSkippingWorkflowSteps() {
        Ticket ticket = newTicket(TicketPriority.LOW);

        assertThatThrownBy(() -> ticket.moveTo(TicketStatus.RESOLVED)).isInstanceOf(ConflictException.class);
        assertThat(ticket.getStatus()).isEqualTo(TicketStatus.OPEN);
    }

    @Test
    void dueDateFollowsPriority() {
        Ticket ticket = newTicket(TicketPriority.LOW);
        assertThat(Duration.between(ticket.getCreatedAt(), ticket.getDueAt())).isEqualTo(Duration.ofHours(72));

        ticket.changePriority(TicketPriority.URGENT);
        assertThat(Duration.between(ticket.getCreatedAt(), ticket.getDueAt())).isEqualTo(Duration.ofHours(4));
    }

    @Test
    void reopeningClearsTheResolvedDate() {
        Ticket ticket = newTicket(TicketPriority.MEDIUM);
        ticket.moveTo(TicketStatus.IN_PROGRESS);
        ticket.moveTo(TicketStatus.RESOLVED);
        assertThat(ticket.getResolvedAt()).isNotNull();

        ticket.moveTo(TicketStatus.IN_PROGRESS);
        assertThat(ticket.getResolvedAt()).isNull();
    }
}
