import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import type { FormEvent } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { ApiError, api } from '../api';
import { ErrorMessage } from '../components';

export function NewTicketPage() {
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');

  const create = useMutation({
    mutationFn: () => api.createTicket(title, description),
    onSuccess: (ticket) => {
      queryClient.invalidateQueries({ queryKey: ['tickets'] });
      navigate(`/tickets/${ticket.id}`);
    },
  });

  const fieldErrors = create.error instanceof ApiError ? create.error.fieldErrors : {};

  function handleSubmit(event: FormEvent) {
    event.preventDefault();
    create.mutate();
  }

  return (
    <>
      <div className="page-header">
        <h1>New ticket</h1>
      </div>
      <form className="card form-narrow" onSubmit={handleSubmit}>
        <label>
          Title
          <input value={title} onChange={(e) => setTitle(e.target.value)} required maxLength={160} />
          {fieldErrors.title && <small className="error">{fieldErrors.title}</small>}
        </label>
        <label>
          What happened?
          <textarea rows={7} value={description} onChange={(e) => setDescription(e.target.value)} required maxLength={5000} />
          {fieldErrors.description && <small className="error">{fieldErrors.description}</small>}
        </label>
        <p className="muted small">The category and priority are set automatically from your description.</p>
        <ErrorMessage error={create.error} />
        <div className="row">
          <button className="primary" disabled={create.isPending}>
            {create.isPending ? 'Submitting…' : 'Submit ticket'}
          </button>
          <Link to="/tickets">Cancel</Link>
        </div>
      </form>
    </>
  );
}
