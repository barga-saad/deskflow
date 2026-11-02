import { useMutation } from '@tanstack/react-query';
import { useState } from 'react';
import type { FormEvent } from 'react';
import { Link, Navigate, useNavigate } from 'react-router-dom';
import { ApiError, api } from '../api';
import { useAuth } from '../auth';
import { ErrorMessage } from '../components';

export function AuthPage({ mode }: { mode: 'login' | 'register' }) {
  const { user, signIn } = useAuth();
  const navigate = useNavigate();
  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const isLogin = mode === 'login';

  const submit = useMutation({
    mutationFn: () => (isLogin ? api.login(email, password) : api.register(fullName, email, password)),
    onSuccess: (auth) => {
      signIn(auth);
      navigate('/tickets');
    },
  });

  if (user) {
    return <Navigate to="/tickets" replace />;
  }

  const fieldErrors = submit.error instanceof ApiError ? submit.error.fieldErrors : {};

  function handleSubmit(event: FormEvent) {
    event.preventDefault();
    submit.mutate();
  }

  return (
    <main className="auth">
      <form className="card" onSubmit={handleSubmit}>
        <h1>DeskFlow</h1>
        <p className="muted">{isLogin ? 'Sign in to your account' : 'Create a customer account'}</p>

        {!isLogin && (
          <label>
            Full name
            <input value={fullName} onChange={(e) => setFullName(e.target.value)} required maxLength={120} />
            {fieldErrors.fullName && <small className="error">{fieldErrors.fullName}</small>}
          </label>
        )}
        <label>
          Email
          <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required autoComplete="email" />
          {fieldErrors.email && <small className="error">{fieldErrors.email}</small>}
        </label>
        <label>
          Password
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            required
            minLength={isLogin ? undefined : 8}
            autoComplete={isLogin ? 'current-password' : 'new-password'}
          />
          {fieldErrors.password && <small className="error">{fieldErrors.password}</small>}
        </label>

        <ErrorMessage error={submit.error} />
        <button className="primary" disabled={submit.isPending}>
          {submit.isPending ? 'Please wait…' : isLogin ? 'Sign in' : 'Create account'}
        </button>
        <p className="muted small">
          {isLogin ? 'New here?' : 'Already registered?'}{' '}
          <Link to={isLogin ? '/register' : '/login'}>{isLogin ? 'Create an account' : 'Sign in'}</Link>
        </p>
      </form>
    </main>
  );
}
