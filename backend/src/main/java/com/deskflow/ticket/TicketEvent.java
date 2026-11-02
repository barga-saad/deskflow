package com.deskflow.ticket;

import com.deskflow.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

/** Audit trail: one row for each change made to a ticket. */
@Entity
@Table(name = "ticket_events")
public class TicketEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "actor_id", nullable = false)
    private User actor;

    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private String detail;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected TicketEvent() {
    }

    public TicketEvent(Ticket ticket, User actor, String type, String detail) {
        this.ticket = ticket;
        this.actor = actor;
        this.type = type;
        this.detail = detail;
        this.createdAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public User getActor() {
        return actor;
    }

    public String getType() {
        return type;
    }

    public String getDetail() {
        return detail;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
