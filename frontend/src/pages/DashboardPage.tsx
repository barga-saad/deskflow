import { useQuery } from '@tanstack/react-query';
import { Link } from 'react-router-dom';
import { PRIORITIES, STATUSES, api } from '../api';
import { ErrorMessage } from '../components';
import { label } from '../format';

export function DashboardPage() {
  const dashboard = useQuery({ queryKey: ['dashboard'], queryFn: api.dashboard });
  const data = dashboard.data;

  return (
    <>
      <div className="page-header">
        <h1>Dashboard</h1>
      </div>
      <ErrorMessage error={dashboard.error} />
      {data && (
        <>
          <div className="stats">
            {STATUSES.map((status) => (
              <Link key={status} className="card stat" to={`/tickets?status=${status}`}>
                <span className="stat-value">{data.byStatus[status] ?? 0}</span>
                <span className="muted">{label(status)}</span>
              </Link>
            ))}
            <Link className="card stat" to="/tickets?overdue=true">
              <span className={data.overdue > 0 ? 'stat-value error' : 'stat-value'}>{data.overdue}</span>
              <span className="muted">Overdue</span>
            </Link>
            <div className="card stat">
              <span className="stat-value">{data.averageResolutionHours ?? '—'}</span>
              <span className="muted">Avg. hours to resolve</span>
            </div>
          </div>

          <section className="card">
            <h2>Active tickets by priority</h2>
            <table>
              <tbody>
                {[...PRIORITIES].reverse().map((priority) => (
                  <tr key={priority}>
                    <td>{label(priority)}</td>
                    <td className="numeric">{data.activeByPriority[priority] ?? 0}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </section>
        </>
      )}
    </>
  );
}
