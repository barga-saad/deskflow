package com.deskflow.ticket;

import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

/** Small query pieces that are combined at runtime, depending on which filters were sent. */
final class TicketSpecifications {

    private TicketSpecifications() {
    }

    static Specification<Ticket> requestedBy(Long userId) {
        return (root, query, cb) -> cb.equal(root.get("requester").get("id"), userId);
    }

    static Specification<Ticket> hasStatus(TicketStatus status) {
        return equalTo("status", status);
    }

    static Specification<Ticket> hasPriority(TicketPriority priority) {
        return equalTo("priority", priority);
    }

    static Specification<Ticket> hasCategory(TicketCategory category) {
        return equalTo("category", category);
    }

    static Specification<Ticket> assignedTo(Long assigneeId) {
        if (assigneeId == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.equal(root.get("assignee").get("id"), assigneeId);
    }

    static Specification<Ticket> overdue(boolean onlyOverdue) {
        if (!onlyOverdue) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.and(
                root.get("status").in(List.of(TicketStatus.OPEN, TicketStatus.IN_PROGRESS)),
                cb.lessThan(root.get("dueAt"), Instant.now()));
    }

    static Specification<Ticket> textContains(String text) {
        if (text == null || text.isBlank()) {
            return Specification.unrestricted();
        }
        String pattern = "%" + escapeLike(text.trim().toLowerCase()) + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("title")), pattern, '\\'),
                cb.like(cb.lower(root.get("description")), pattern, '\\'));
    }

    private static Specification<Ticket> equalTo(String field, Object value) {
        if (value == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.equal(root.get(field), value);
    }

    private static String escapeLike(String text) {
        return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
