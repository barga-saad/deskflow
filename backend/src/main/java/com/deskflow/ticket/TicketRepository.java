package com.deskflow.ticket;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface TicketRepository extends JpaRepository<Ticket, Long>, JpaSpecificationExecutor<Ticket> {

    /** Loads requester and assignee in the same query, which avoids the N+1 problem on lists. */
    @Override
    @EntityGraph(attributePaths = {"requester", "assignee"})
    Page<Ticket> findAll(Specification<Ticket> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"requester", "assignee"})
    Optional<Ticket> findWithPeopleById(Long id);

    interface StatusCount {
        TicketStatus getStatus();

        long getCount();
    }

    interface PriorityCount {
        TicketPriority getPriority();

        long getCount();
    }

    @Query("select t.status as status, count(t) as count from Ticket t group by t.status")
    List<StatusCount> countByStatus();

    @Query("""
            select t.priority as priority, count(t) as count from Ticket t
            where t.status in (com.deskflow.ticket.TicketStatus.OPEN, com.deskflow.ticket.TicketStatus.IN_PROGRESS)
            group by t.priority
            """)
    List<PriorityCount> countActiveByPriority();

    @Query("""
            select count(t) from Ticket t
            where t.status in (com.deskflow.ticket.TicketStatus.OPEN, com.deskflow.ticket.TicketStatus.IN_PROGRESS)
              and t.dueAt < :now
            """)
    long countOverdue(Instant now);

    @Query("select t.createdAt as createdAt, t.resolvedAt as resolvedAt from Ticket t where t.resolvedAt is not null")
    List<ResolutionTimes> findResolutionTimes();

    interface ResolutionTimes {
        Instant getCreatedAt();

        Instant getResolvedAt();
    }
}
