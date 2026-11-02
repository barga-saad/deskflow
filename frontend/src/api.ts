export type Role = 'CUSTOMER' | 'AGENT' | 'ADMIN';
export type TicketStatus = 'OPEN' | 'IN_PROGRESS' | 'RESOLVED' | 'CLOSED';
export type TicketPriority = 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';
export type TicketCategory = 'BILLING' | 'TECHNICAL' | 'ACCOUNT' | 'GENERAL';

export const STATUSES: TicketStatus[] = ['OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED'];
export const PRIORITIES: TicketPriority[] = ['LOW', 'MEDIUM', 'HIGH', 'URGENT'];
export const CATEGORIES: TicketCategory[] = ['BILLING', 'TECHNICAL', 'ACCOUNT', 'GENERAL'];

export interface User {
  id: number;
  email: string;
  fullName: string;
  role: Role;
}

export interface AuthResponse {
  token: string;
  user: User;
}

export interface Person {
  id: number;
  fullName: string;
}

export interface Ticket {
  id: number;
  title: string;
  description: string;
  status: TicketStatus;
  priority: TicketPriority;
  category: TicketCategory;
  triageNote: string | null;
  triagedByAi: boolean;
  requester: Person;
  assignee: Person | null;
  dueAt: string;
  overdue: boolean;
  resolvedAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface Comment {
  id: number;
  author: Person;
  body: string;
  internal: boolean;
  createdAt: string;
}

export interface TicketEvent {
  id: number;
  actor: Person;
  type: string;
  detail: string;
  createdAt: string;
}

export interface Page<T> {
  items: T[];
  page: number;
  size: number;
  totalItems: number;
  totalPages: number;
}

export interface Dashboard {
  byStatus: Partial<Record<TicketStatus, number>>;
  activeByPriority: Partial<Record<TicketPriority, number>>;
  overdue: number;
  averageResolutionHours: number | null;
}

export class ApiError extends Error {
  constructor(
    message: string,
    readonly status: number,
    readonly fieldErrors: Record<string, string> = {},
  ) {
    super(message);
  }
}

const TOKEN_KEY = 'deskflow.token';
const BASE_URL = import.meta.env.VITE_API_URL ?? '';

export const tokenStore = {
  get: () => localStorage.getItem(TOKEN_KEY),
  set: (token: string) => localStorage.setItem(TOKEN_KEY, token),
  clear: () => localStorage.removeItem(TOKEN_KEY),
};

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const token = tokenStore.get();
  const response = await fetch(`${BASE_URL}/api${path}`, {
    ...init,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
  });

  if (response.ok) {
    return (response.status === 204 ? undefined : await response.json()) as T;
  }

  // An expired session sends the user back to the login page.
  if (response.status === 401 && token) {
    tokenStore.clear();
    window.location.assign('/login');
  }
  const problem = await response.json().catch(() => ({}));
  throw new ApiError(problem.detail ?? 'Something went wrong', response.status, problem.errors);
}

const post = <T>(path: string, body: unknown) =>
  request<T>(path, { method: 'POST', body: JSON.stringify(body) });

const patch = <T>(path: string, body: unknown) =>
  request<T>(path, { method: 'PATCH', body: JSON.stringify(body) });

export const api = {
  login: (email: string, password: string) => post<AuthResponse>('/auth/login', { email, password }),
  register: (fullName: string, email: string, password: string) =>
    post<AuthResponse>('/auth/register', { fullName, email, password }),
  me: () => request<User>('/auth/me'),

  tickets: (params: URLSearchParams) => request<Page<Ticket>>(`/tickets?${params}`),
  ticket: (id: string) => request<Ticket>(`/tickets/${id}`),
  createTicket: (title: string, description: string) => post<Ticket>('/tickets', { title, description }),
  updateTicket: (id: string, changes: Record<string, unknown>) => patch<Ticket>(`/tickets/${id}`, changes),

  comments: (id: string) => request<Comment[]>(`/tickets/${id}/comments`),
  addComment: (id: string, body: string, internal: boolean) =>
    post<Comment>(`/tickets/${id}/comments`, { body, internal }),
  events: (id: string) => request<TicketEvent[]>(`/tickets/${id}/events`),

  dashboard: () => request<Dashboard>('/dashboard'),
  users: () => request<User[]>('/users'),
  agents: () => request<User[]>('/users/agents'),
  updateRole: (id: number, role: Role) => patch<User>(`/users/${id}/role`, { role }),
};
