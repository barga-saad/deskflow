package com.deskflow.ticket;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    public record DashboardResponse(
            Map<TicketStatus, Long> byStatus,
            Map<TicketPriority, Long> activeByPriority,
            long overdue,
            Double averageResolutionHours) {
    }

    private final TicketRepository tickets;

    public DashboardController(TicketRepository tickets) {
        this.tickets = tickets;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
    @Transactional(readOnly = true)
    public DashboardResponse summary() {
        Map<TicketStatus, Long> byStatus = tickets.countByStatus().stream()
                .collect(Collectors.toMap(TicketRepository.StatusCount::getStatus, TicketRepository.StatusCount::getCount));
        Map<TicketPriority, Long> byPriority = tickets.countActiveByPriority().stream()
                .collect(Collectors.toMap(TicketRepository.PriorityCount::getPriority, TicketRepository.PriorityCount::getCount));
        return new DashboardResponse(byStatus, byPriority, tickets.countOverdue(Instant.now()),
                averageHours(tickets.findResolutionTimes()));
    }

    private static Double averageHours(List<TicketRepository.ResolutionTimes> times) {
        if (times.isEmpty()) {
            return null;
        }
        double averageMinutes = times.stream()
                .mapToLong(t -> Duration.between(t.getCreatedAt(), t.getResolvedAt()).toMinutes())
                .average()
                .orElse(0);
        return Math.round(averageMinutes / 6.0) / 10.0;
    }
}
