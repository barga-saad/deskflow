package com.deskflow.ticket;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TicketEventRepository extends JpaRepository<TicketEvent, Long> {

    @EntityGraph(attributePaths = "actor")
    List<TicketEvent> findByTicketIdOrderByCreatedAtDesc(Long ticketId);
}
