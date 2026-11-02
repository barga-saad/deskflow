import { NavLink, Navigate, Outlet, Route, Routes, useNavigate } from 'react-router-dom';
import { useAuth } from './auth';
import { AuthPage } from './pages/AuthPage';
import { DashboardPage } from './pages/DashboardPage';
import { NewTicketPage } from './pages/NewTicketPage';
import { TeamPage } from './pages/TeamPage';
import { TicketDetailPage } from './pages/TicketDetailPage';
import { TicketsPage } from './pages/TicketsPage';

function Layout() {
  const { user, isStaff, loading, signOut } = useAuth();
  const navigate = useNavigate();

  if (loading) {
    return <p className="center muted">Loading…</p>;
  }
  if (!user) {
    return <Navigate to="/login" replace />;
  }

  return (
    <>
      <header className="topbar">
        <NavLink to="/tickets" className="brand">DeskFlow</NavLink>
        <nav>
          <NavLink to="/tickets">Tickets</NavLink>
          {isStaff && <NavLink to="/dashboard">Dashboard</NavLink>}
          {user.role === 'ADMIN' && <NavLink to="/team">Team</NavLink>}
        </nav>
        <div className="topbar-user">
          <span className="muted">{user.fullName}</span>
          <button
            className="link"
            onClick={() => {
              signOut();
              navigate('/login');
            }}
          >
            Sign out
          </button>
        </div>
      </header>
      <main className="page">
        <Outlet />
      </main>
    </>
  );
}

function StaffOnly() {
  const { isStaff } = useAuth();
  return isStaff ? <Outlet /> : <Navigate to="/tickets" replace />;
}

export function App() {
  return (
    <Routes>
      <Route path="/login" element={<AuthPage mode="login" />} />
      <Route path="/register" element={<AuthPage mode="register" />} />
      <Route element={<Layout />}>
        <Route path="/tickets" element={<TicketsPage />} />
        <Route path="/tickets/new" element={<NewTicketPage />} />
        <Route path="/tickets/:id" element={<TicketDetailPage />} />
        <Route element={<StaffOnly />}>
          <Route path="/dashboard" element={<DashboardPage />} />
          <Route path="/team" element={<TeamPage />} />
        </Route>
      </Route>
      <Route path="*" element={<Navigate to="/tickets" replace />} />
    </Routes>
  );
}
