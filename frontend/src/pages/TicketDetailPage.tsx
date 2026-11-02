import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import type { FormEvent } from 'react';
import { Link, useParams } from 'react-router-dom';
import { CATEGORIES, PRIORITIES, api } from '../api';
import type { Ticket, TicketStatus } from '../api';
import { useAuth } from '../auth';
import { ErrorMessage, PriorityBadge, StatusBadge } from '../components';
import { dueLabel, formatDateTime, label } from '../format';

const NEXT_STATUSES: Record<TicketStatus, TicketStatus[]> = {
  OPEN: ['IN_PROGRESS', 'CLOSED'],
  IN_PROGRESS: ['RESOLVED', 'OPEN'],
  RESOLVED: ['CLOSED', 'IN_PROGRESS'],
  CLOSED: [],
};

function StaffPanel({ ticket }: { ticket: Ticket }) {
  const id = String(ticket.id);
  const queryClient = useQueryClient();
  const agents = useQuery({ queryKey: ['agents'], queryFn: api.agents });
  const events = useQuery({ queryKey: ['ticket', id, 'events'], queryFn: () => api.events(id) });

  const update = useMutation({
    mutationFn: (changes: Record<string, unknown>) => api.updateTicket(id, changes),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['ticket', id] });
      queryClient.invalidateQueries({ queryKey: ['tickets'] });
    },
  });

  return (
    <aside className="stack">
      <section className="card">
        <h2>Manage</h2>
        <div className="row wrap">
          {NEXT_STATUSES[ticket.status].map((status) => (
            <button key={status} disabled={update.isPending} onClick={() => update.mutate({ status })}>
              Move to {label(status).toLowerCase()}
            </button>
          ))}
        </div>
        <label>
          Assignee
          <select
            value={ticket.assignee?.id ?? ''}
            onChange={(e) => update.mutate(e.target.value ? { assigneeId: Number(e.target.value) } : { unassign: true })}
          >
            <option value="">Unassigned</option>
            {agents.data?.map((agent) => <option key={agent.id} value={agent.id}>{agent.fullName}</option>)}
          </select>
        </label>
        <label>
          Priority
          <select value={ticket.priority} onChange={(e) => update.mutate({ priority: e.target.value })}>
            {PRIORITIES.map((p) => <option key={p} value={p}>{label(p)}</option>)}
          </select>
        </label>
        <label>
          Category
          <select value={ticket.category} onChange={(e) => update.mutate({ category: e.target.value })}>
            {CATEGORIES.map((c) => <option key={c} value={c}>{label(c)}</option>)}
          </select>
        </label>
        <ErrorMessage error={update.error} />
      </section>

      <section className="card">
        <h2>History</h2>
        <ul className="timeline">
          {events.data?.map((event) => (
            <li key={event.id}>
              <div>{event.detail}</div>
              <div className="muted small">{event.actor.fullName} · {formatDateTime(event.createdAt)}</div>
            </li>
          ))}
        </ul>
      </section>
    </aside>
  );
}

export function TicketDetailPage() {
  const id = useParams().id!;
  const { isStaff } = useAuth();
  const queryClient = useQueryClient();
  const [body, setBody] = useState('');
  const [internal, setInternal] = useState(false);

  const ticket = useQuery({ queryKey: ['ticket', id], queryFn: () => api.ticket(id) });
  const comments = useQuery({ queryKey: ['ticket', id, 'comments'], queryFn: () => api.comments(id) });

  const addComment = useMutation({
    mutationFn: () => api.addComment(id, body, internal),
    onSuccess: () => {
      setBody('');
      setInternal(false);
      queryClient.invalidateQueries({ queryKey: ['ticket', id, 'comments'] });
    },
  });

  function handleSubmit(event: FormEvent) {
    event.preventDefault();
    addComment.mutate();
  }

  if (ticket.error) {
    return (
      <>
        <ErrorMessage error={ticket.error} />
        <Link to="/tickets">Back to tickets</Link>
      </>
    );
  }
  if (!ticket.data) {
    return <p className="muted">Loading…</p>;
  }

  const current = ticket.data;
  const active = current.status === 'OPEN' || current.status === 'IN_PROGRESS';

  return (
    <>
      <Link className="small" to="/tickets">← Back to tickets</Link>
      <div className="page-header">
        <h1>{current.title}</h1>
      </div>
      <div className="row wrap meta">
        <StatusBadge status={current.status} />
        <PriorityBadge priority={current.priority} />
        <span className="badge">{label(current.category)}</span>
        {active && <span className={current.overdue ? 'error small' : 'muted small'}>{dueLabel(current.dueAt)}</span>}
      </div>

      <div className={isStaff ? 'detail-grid' : ''}>
        <div className="stack">
          <section className="card">
            <p className="muted small">
              #{current.id} · {current.requester.fullName} · {formatDateTime(current.createdAt)}
            </p>
            <p className="prewrap">{current.description}</p>
            {isStaff && current.triageNote && (
              <p className="triage small">
                <strong>{current.triagedByAi ? 'AI triage' : 'Automatic triage'}:</strong> {current.triageNote}
              </p>
            )}
          </section>

          <section className="card">
            <h2>Conversation</h2>
            <ErrorMessage error={comments.error} />
            {comments.data?.length === 0 && <p className="muted">No replies yet.</p>}
            {comments.data?.map((comment) => (
              <article key={comment.id} className={comment.internal ? 'comment internal' : 'comment'}>
                <div className="muted small">
                  {comment.author.fullName} · {formatDateTime(comment.createdAt)}
                  {comment.internal && ' · Internal note'}
                </div>
                <p className="prewrap">{comment.body}</p>
              </article>
            ))}

            {current.status === 'CLOSED' ? (
              <p className="muted small">This ticket is closed.</p>
            ) : (
              <form onSubmit={handleSubmit}>
                <label>
                  Reply
                  <textarea rows={3} value={body} onChange={(e) => setBody(e.target.value)} required maxLength={5000} />
                </label>
                <ErrorMessage error={addComment.error} />
                <div className="row">
                  <button className="primary" disabled={addComment.isPending}>Send</button>
                  {isStaff && (
                    <label className="checkbox">
                      <input type="checkbox" checked={internal} onChange={(e) => setInternal(e.target.checked)} />
                      Internal note
                    </label>
                  )}
                </div>
              </form>
            )}
          </section>
        </div>

        {isStaff && <StaffPanel ticket={current} />}
      </div>
    </>
  );
}
