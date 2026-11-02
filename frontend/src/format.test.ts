import { describe, expect, it } from 'vitest';
import { dueLabel, label } from './format';

describe('label', () => {
  it('makes API values readable', () => {
    expect(label('IN_PROGRESS')).toBe('In progress');
    expect(label('OPEN')).toBe('Open');
  });
});

describe('dueLabel', () => {
  const now = new Date('2026-01-10T12:00:00Z');

  it('shows the time left', () => {
    expect(dueLabel('2026-01-10T12:30:00Z', now)).toBe('due in 30m');
    expect(dueLabel('2026-01-10T16:00:00Z', now)).toBe('due in 4h');
    expect(dueLabel('2026-01-13T12:00:00Z', now)).toBe('due in 3d');
  });

  it('shows how late a ticket is', () => {
    expect(dueLabel('2026-01-10T10:00:00Z', now)).toBe('2h overdue');
  });
});
