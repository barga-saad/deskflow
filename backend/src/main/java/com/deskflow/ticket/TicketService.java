package com.deskflow.ticket;

import com.deskflow.ai.TriageResult;
import com.deskflow.ai.TriageService;
import com.deskflow.common.ConflictException;
import com.deskflow.common.NotFoundException;
import com.deskflow.common.PageResponse;
import com.deskflow.security.AuthenticatedUser;
import com.deskflow.ticket.TicketDtos.CommentResponse;
import com.deskflow.ticket.TicketDtos.CreateCommentRequest;
import com.deskflow.ticket.TicketDtos.CreateTicketRequest;
import com.deskflow.ticket.TicketDtos.EventResponse;
import com.deskflow.ticket.TicketDtos.TicketQuery;
import com.deskflow.ticket.TicketDtos.TicketResponse;
import com.deskflow.ticket.TicketDtos.UpdateTicketRequest;
import com.deskflow.user.Role;
import com.deskflow.user.User;
import com.deskflow.user.UserRepository;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class TicketService {

    private final TicketRepository tickets;
    private final TicketCommentRepository comments;
    private final TicketEventRepository events;
    private final UserRepository users;
    private final TriageService triageService;
    private final TransactionTemplate transaction;

    public TicketService(TicketRepository tickets, TicketCommentRepository comments,
                         TicketEventRepository events, UserRepository users,
                         TriageService triageService, TransactionTemplate transaction) {
        this.tickets = tickets;
        this.comments = comments;
        this.events = events;
        this.users = users;
        this.triageService = triageService;
        this.transaction = transaction;
    }

    public TicketResponse create(CreateTicketRequest request, AuthenticatedUser currentUser) {
        // The AI call can take seconds, so it runs before the transaction and holds no database connection.
        TriageResult triage = triageService.triage(request.title(), request.description());

        return transaction.execute(status -> {
            User requester = loadUser(currentUser.id());
            Ticket ticket = tickets.save(new Ticket(request.title().trim(), request.description().trim(), requester,
                    triage.priority(), triage.category(), triage.note(), triage.byAi()));
            record(ticket, requester, "CREATED", "Ticket created, triaged as %s / %s"
                    .formatted(triage.category(), triage.priority()));
            return TicketResponse.from(ticket);
        });
    }

    @Transactional(readOnly = true)
    public PageResponse<TicketResponse> search(TicketQuery query, Pageable pageable, AuthenticatedUser currentUser) {
        Specification<Ticket> spec = Specification.allOf(
                currentUser.isStaff() ? Specification.unrestricted() : TicketSpecifications.requestedBy(currentUser.id()),
                TicketSpecifications.hasStatus(query.status()),
                TicketSpecifications.hasPriority(query.priority()),
                TicketSpecifications.hasCategory(query.category()),
                TicketSpecifications.assignedTo(query.assigneeId()),
                TicketSpecifications.overdue(Boolean.TRUE.equals(query.overdue())),
                TicketSpecifications.textContains(query.q()));

        return PageResponse.from(tickets.findAll(spec, pageable).map(TicketResponse::from));
    }

    @Transactional(readOnly = true)
    public TicketResponse get(Long id, AuthenticatedUser currentUser) {
        return TicketResponse.from(loadVisibleTicket(id, currentUser));
    }

    @Transactional
    public TicketResponse update(Long id, UpdateTicketRequest request, AuthenticatedUser currentUser) {
        Ticket ticket = loadVisibleTicket(id, currentUser);
        User actor = loadUser(currentUser.id());

        if (request.status() != null && request.status() != ticket.getStatus()) {
            TicketStatus previous = ticket.getStatus();
            ticket.moveTo(request.status());
            record(ticket, actor, "STATUS_CHANGED", previous + " to " + request.status());
        }
        if (request.priority() != null && request.priority() != ticket.getPriority()) {
            TicketPriority previous = ticket.getPriority();
            ticket.changePriority(request.priority());
            record(ticket, actor, "PRIORITY_CHANGED", previous + " to " + request.priority());
        }
        if (request.category() != null && request.category() != ticket.getCategory()) {
            TicketCategory previous = ticket.getCategory();
            ticket.changeCategory(request.category());
            record(ticket, actor, "CATEGORY_CHANGED", previous + " to " + request.category());
        }
        if (Boolean.TRUE.equals(request.unassign())) {
            ticket.assignTo(null);
            record(ticket, actor, "UNASSIGNED", "Ticket unassigned");
        } else if (request.assigneeId() != null) {
            User agent = loadUser(request.assigneeId());
            if (agent.getRole() == Role.CUSTOMER) {
                throw new ConflictException("Tickets can only be assigned to staff");
            }
            ticket.assignTo(agent);
            record(ticket, actor, "ASSIGNED", "Assigned to " + agent.getFullName());
        }
        return TicketResponse.from(ticket);
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> listComments(Long ticketId, AuthenticatedUser currentUser) {
        loadVisibleTicket(ticketId, currentUser);
        List<TicketComment> found = currentUser.isStaff()
                ? comments.findByTicketIdOrderByCreatedAt(ticketId)
                : comments.findByTicketIdAndInternalFalseOrderByCreatedAt(ticketId);
        return found.stream().map(CommentResponse::from).toList();
    }

    @Transactional
    public CommentResponse addComment(Long ticketId, CreateCommentRequest request, AuthenticatedUser currentUser) {
        Ticket ticket = loadVisibleTicket(ticketId, currentUser);
        if (request.internal() && !currentUser.isStaff()) {
            throw new AccessDeniedException("Only staff can write internal notes");
        }
        if (ticket.getStatus() == TicketStatus.CLOSED) {
            throw new ConflictException("Closed tickets cannot receive comments");
        }
        TicketComment comment = new TicketComment(ticket, loadUser(currentUser.id()),
                request.body().trim(), request.internal());
        return CommentResponse.from(comments.save(comment));
    }

    @Transactional(readOnly = true)
    public List<EventResponse> listEvents(Long ticketId, AuthenticatedUser currentUser) {
        loadVisibleTicket(ticketId, currentUser);
        return events.findByTicketIdOrderByCreatedAtDesc(ticketId).stream().map(EventResponse::from).toList();
    }

    /** A customer asking for someone else's ticket gets 404, so ticket ids reveal nothing. */
    private Ticket loadVisibleTicket(Long id, AuthenticatedUser currentUser) {
        return tickets.findWithPeopleById(id)
                .filter(ticket -> currentUser.isStaff() || ticket.getRequester().getId().equals(currentUser.id()))
                .orElseThrow(() -> new NotFoundException("Ticket not found"));
    }

    private User loadUser(Long id) {
        return users.findById(id).orElseThrow(() -> new NotFoundException("User not found"));
    }

    private void record(Ticket ticket, User actor, String type, String detail) {
        events.save(new TicketEvent(ticket, actor, type, detail));
    }
}
