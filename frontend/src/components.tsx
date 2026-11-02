import type { TicketPriority, TicketStatus } from './api';
import { label } from './format';

export function StatusBadge({ status }: { status: TicketStatus }) {
  return <span className={`badge status-${status.toLowerCase()}`}>{label(status)}</span>;
}

export function PriorityBadge({ priority }: { priority: TicketPriority }) {
  return <span className={`badge priority-${priority.toLowerCase()}`}>{label(priority)}</span>;
}

export function ErrorMessage({ error }: { error: unknown }) {
  if (!error) {
    return null;
  }
  return <p className="error" role="alert">{error instanceof Error ? error.message : 'Something went wrong'}</p>;
}
