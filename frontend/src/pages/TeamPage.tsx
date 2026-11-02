import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { Navigate } from 'react-router-dom';
import { api } from '../api';
import type { Role, User } from '../api';
import { useAuth } from '../auth';
import { ErrorMessage } from '../components';
import { label } from '../format';

const ROLES: Role[] = ['CUSTOMER', 'AGENT', 'ADMIN'];

export function TeamPage() {
  const { user: currentUser } = useAuth();
  const queryClient = useQueryClient();
  const users = useQuery({ queryKey: ['users'], queryFn: api.users, enabled: currentUser?.role === 'ADMIN' });

  const updateRole = useMutation({
    mutationFn: ({ user, role }: { user: User; role: Role }) => api.updateRole(user.id, role),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['users'] }),
  });

  if (currentUser?.role !== 'ADMIN') {
    return <Navigate to="/tickets" replace />;
  }

  return (
    <>
      <div className="page-header">
        <h1>Team</h1>
      </div>
      <ErrorMessage error={users.error ?? updateRole.error} />
      <div className="table-wrap card flush">
        <table>
          <thead>
            <tr>
              <th>Name</th>
              <th>Email</th>
              <th>Role</th>
            </tr>
          </thead>
          <tbody>
            {users.data?.map((user) => (
              <tr key={user.id}>
                <td>{user.fullName}</td>
                <td className="muted">{user.email}</td>
                <td>
                  <select
                    aria-label={`Role for ${user.fullName}`}
                    value={user.role}
                    disabled={user.id === currentUser.id || updateRole.isPending}
                    onChange={(e) => updateRole.mutate({ user, role: e.target.value as Role })}
                  >
                    {ROLES.map((role) => <option key={role} value={role}>{label(role)}</option>)}
                  </select>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </>
  );
}
