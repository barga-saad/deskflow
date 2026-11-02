import { keepPreviousData, useQuery } from '@tanstack/react-query';
import { Link, useSearchParams } from 'react-router-dom';
import { CATEGORIES, PRIORITIES, STATUSES, api } from '../api';
import { useAuth } from '../auth';
import { ErrorMessage, PriorityBadge, StatusBadge } from '../components';
import { dueLabel, label } from '../format';

export function TicketsPage() {
  const { isStaff } = useAuth();
  // Filters live in the URL, so a filtered list can be refreshed or shared as a link.
  const [params, setParams] = useSearchParams();
  const page = Number(params.get('page') ?? 0);

  const tickets = useQuery({
    queryKey: ['tickets', params.toString()],
    queryFn: () => api.tickets(params),
    placeholderData: keepPreviousData,
  });

  function setFilter(name: string, value: string) {
    const next = new URLSearchParams(params);
    if (value) {
      next.set(name, value);
    } else {
      next.delete(name);
    }
    if (name !== 'page') {
      next.delete('page');
    }
    setParams(next);
  }

  return (
    <>
      <div className="page-header">
        <h1>Tickets</h1>
        <Link className="button primary" to="/tickets/new">New ticket</Link>
      </div>

      <div className="filters">
        <input
          type="search"
          placeholder="Search tickets"
          aria-label="Search tickets"
          defaultValue={params.get('q') ?? ''}
          onKeyDown={(e) => e.key === 'Enter' && setFilter('q', e.currentTarget.value)}
        />
        <select aria-label="Status" value={params.get('status') ?? ''} onChange={(e) => setFilter('status', e.target.value)}>
          <option value="">All statuses</option>
          {STATUSES.map((s) => <option key={s} value={s}>{label(s)}</option>)}
        </select>
        <select aria-label="Priority" value={params.get('priority') ?? ''} onChange={(e) => setFilter('priority', e.target.value)}>
          <option value="">All priorities</option>
          {PRIORITIES.map((p) => <option key={p} value={p}>{label(p)}</option>)}
        </select>
        <select aria-label="Category" value={params.get('category') ?? ''} onChange={(e) => setFilter('category', e.target.value)}>
          <option value="">All categories</option>
          {CATEGORIES.map((c) => <option key={c} value={c}>{label(c)}</option>)}
        </select>
        {isStaff && (
          <label className="checkbox">
            <input
              type="checkbox"
              checked={params.get('overdue') === 'true'}
              onChange={(e) => setFilter('overdue', e.target.checked ? 'true' : '')}
            />
            Overdue only
          </label>
        )}
      </div>

      <ErrorMessage error={tickets.error} />

      {tickets.data && tickets.data.items.length === 0 && (
        <p className="empty muted">No tickets match. Create one, or clear the filters.</p>
      )}

      {tickets.data && tickets.data.items.length > 0 && (
        <div className="table-wrap card flush">
          <table>
            <thead>
              <tr>
                <th>Ticket</th>
                <th>Status</th>
                <th>Priority</th>
                <th>Category</th>
                {isStaff && <th>Assignee</th>}
                <th>Response</th>
              </tr>
            </thead>
            <tbody>
              {tickets.data.items.map((ticket) => (
                <tr key={ticket.id}>
                  <td>
                    <Link to={`/tickets/${ticket.id}`}>{ticket.title}</Link>
                    <div className="muted small">#{ticket.id} · {ticket.requester.fullName}</div>
                  </td>
                  <td><StatusBadge status={ticket.status} /></td>
                  <td><PriorityBadge priority={ticket.priority} /></td>
                  <td>{label(ticket.category)}</td>
                  {isStaff && <td>{ticket.assignee?.fullName ?? <span className="muted">Unassigned</span>}</td>}
                  <td className={ticket.overdue ? 'error' : 'muted'}>
                    {ticket.status === 'OPEN' || ticket.status === 'IN_PROGRESS' ? dueLabel(ticket.dueAt) : '—'}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}

      {tickets.data && tickets.data.totalPages > 1 && (
        <div className="pagination">
          <button disabled={page === 0} onClick={() => setFilter('page', String(page - 1))}>Previous</button>
          <span className="muted">Page {page + 1} of {tickets.data.totalPages}</span>
          <button disabled={page + 1 >= tickets.data.totalPages} onClick={() => setFilter('page', String(page + 1))}>Next</button>
        </div>
      )}
    </>
  );
}
