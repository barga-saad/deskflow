/** Turns an API value such as IN_PROGRESS into a label such as "In progress". */
export function label(value: string): string {
  const text = value.replaceAll('_', ' ').toLowerCase();
  return text.charAt(0).toUpperCase() + text.slice(1);
}

export function formatDateTime(value: string): string {
  return new Date(value).toLocaleString(undefined, { dateStyle: 'medium', timeStyle: 'short' });
}

export function dueLabel(dueAt: string, now: Date = new Date()): string {
  const minutes = Math.round((new Date(dueAt).getTime() - now.getTime()) / 60000);
  const absolute = Math.abs(minutes);
  const amount = absolute >= 1440
    ? `${Math.round(absolute / 1440)}d`
    : absolute >= 60
      ? `${Math.round(absolute / 60)}h`
      : `${absolute}m`;
  return minutes < 0 ? `${amount} overdue` : `due in ${amount}`;
}
