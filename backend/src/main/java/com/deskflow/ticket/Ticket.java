package com.deskflow.ticket;

import com.deskflow.common.ConflictException;
import com.deskflow.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "tickets")
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String description;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false)
    private TicketStatus status = TicketStatus.OPEN;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false)
    private TicketPriority priority;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false)
    private TicketCategory category;

    @Column(name = "triage_note")
    private String triageNote;

    @Column(name = "triaged_by_ai", nullable = false)
    private boolean triagedByAi;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private User assignee;

    @Column(name = "due_at", nullable = false)
    private Instant dueAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Optimistic lock: a save fails if another agent changed the ticket in the meantime. */
    @Version
    private long version;

    protected Ticket() {
    }

    public Ticket(String title, String description, User requester, TicketPriority priority,
                  TicketCategory category, String triageNote, boolean triagedByAi) {
        this.title = title;
        this.description = description;
        this.requester = requester;
        this.category = category;
        this.triageNote = triageNote;
        this.triagedByAi = triagedByAi;
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
        applyPriority(priority);
    }

    public void moveTo(TicketStatus next) {
        if (!status.canMoveTo(next)) {
            throw new ConflictException("A ticket cannot go from %s to %s".formatted(status, next));
        }
        status = next;
        resolvedAt = next == TicketStatus.RESOLVED ? Instant.now() : resolvedAt;
        if (next == TicketStatus.IN_PROGRESS || next == TicketStatus.OPEN) {
            resolvedAt = null;
        }
    }

    public void changePriority(TicketPriority next) {
        applyPriority(next);
    }

    private void applyPriority(TicketPriority next) {
        priority = next;
        dueAt = createdAt.plus(next.responseTarget());
    }

    public void changeCategory(TicketCategory next) {
        category = next;
    }

    public void assignTo(User agent) {
        assignee = agent;
    }

    public boolean isOverdue() {
        return status.isActive() && Instant.now().isAfter(dueAt);
    }

    @PreUpdate
    void touch() {
        updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public TicketStatus getStatus() {
        return status;
    }

    public TicketPriority getPriority() {
        return priority;
    }

    public TicketCategory getCategory() {
        return category;
    }

    public String getTriageNote() {
        return triageNote;
    }

    public boolean isTriagedByAi() {
        return triagedByAi;
    }

    public User getRequester() {
        return requester;
    }

    public User getAssignee() {
        return assignee;
    }

    public Instant getDueAt() {
        return dueAt;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public long getVersion() {
        return version;
    }
}
