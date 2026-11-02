package com.deskflow.ticket;

import com.deskflow.common.PageResponse;
import com.deskflow.security.AuthenticatedUser;
import com.deskflow.ticket.TicketDtos.CommentResponse;
import com.deskflow.ticket.TicketDtos.CreateCommentRequest;
import com.deskflow.ticket.TicketDtos.CreateTicketRequest;
import com.deskflow.ticket.TicketDtos.EventResponse;
import com.deskflow.ticket.TicketDtos.TicketQuery;
import com.deskflow.ticket.TicketDtos.TicketResponse;
import com.deskflow.ticket.TicketDtos.UpdateTicketRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @GetMapping
    public PageResponse<TicketResponse> search(TicketQuery query,
                                               @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
                                               @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ticketService.search(query, pageable, currentUser);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TicketResponse create(@Valid @RequestBody CreateTicketRequest request,
                                 @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ticketService.create(request, currentUser);
    }

    @GetMapping("/{id}")
    public TicketResponse get(@PathVariable Long id, @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ticketService.get(id, currentUser);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    public TicketResponse update(@PathVariable Long id,
                                 @RequestBody UpdateTicketRequest request,
                                 @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ticketService.update(id, request, currentUser);
    }

    @GetMapping("/{id}/comments")
    public List<CommentResponse> comments(@PathVariable Long id,
                                          @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ticketService.listComments(id, currentUser);
    }

    @PostMapping("/{id}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentResponse addComment(@PathVariable Long id,
                                      @Valid @RequestBody CreateCommentRequest request,
                                      @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ticketService.addComment(id, request, currentUser);
    }

    @GetMapping("/{id}/events")
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    public List<EventResponse> events(@PathVariable Long id,
                                      @AuthenticationPrincipal AuthenticatedUser currentUser) {
        return ticketService.listEvents(id, currentUser);
    }
}
