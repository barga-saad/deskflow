package com.deskflow.ticket;

import com.deskflow.user.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class TicketDtos {

    private TicketDtos() {
    }

    public record CreateTicketRequest(
            @NotBlank @Size(max = 160) String title,
            @NotBlank @Size(max = 5000) String description) {
    }

    /** Only the fields that are present get changed. */
    public record UpdateTicketRequest(
            TicketStatus status,
            TicketPriority priority,
            TicketCategory category,
            Long assigneeId,
            Boolean unassign) {
    }

    public record CreateCommentRequest(
            @NotBlank @Size(max = 5000) String body,
            boolean internal) {
    }

    public record TicketQuery(
            TicketStatus status,
            TicketPriority priority,
            TicketCategory category,
            Long assigneeId,
            String q,
            Boolean overdue) {
    }

    public record PersonSummary(Long id, String fullName) {

        static PersonSummary from(User user) {
            return user == null ? null : new PersonSummary(user.getId(), user.getFullName());
        }
    }

    public record TicketResponse(
            Long id,
            String title,
            String description,
            TicketStatus status,
            TicketPriority priority,
            TicketCategory category,
            String triageNote,
            boolean triagedByAi,
            PersonSummary requester,
            PersonSummary assignee,
            Instant dueAt,
            boolean overdue,
            Instant resolvedAt,
            Instant createdAt,
            Instant updatedAt) {

        static TicketResponse from(Ticket ticket) {
            return new TicketResponse(ticket.getId(), ticket.getTitle(), ticket.getDescription(),
                    ticket.getStatus(), ticket.getPriority(), ticket.getCategory(),
                    ticket.getTriageNote(), ticket.isTriagedByAi(),
                    PersonSummary.from(ticket.getRequester()), PersonSummary.from(ticket.getAssignee()),
                    ticket.getDueAt(), ticket.isOverdue(), ticket.getResolvedAt(),
                    ticket.getCreatedAt(), ticket.getUpdatedAt());
        }
    }

    public record CommentResponse(Long id, PersonSummary author, String body, boolean internal, Instant createdAt) {

        static CommentResponse from(TicketComment comment) {
            return new CommentResponse(comment.getId(), PersonSummary.from(comment.getAuthor()),
                    comment.getBody(), comment.isInternal(), comment.getCreatedAt());
        }
    }

    public record EventResponse(Long id, PersonSummary actor, String type, String detail, Instant createdAt) {

        static EventResponse from(TicketEvent event) {
            return new EventResponse(event.getId(), PersonSummary.from(event.getActor()),
                    event.getType(), event.getDetail(), event.getCreatedAt());
        }
    }
}
