import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it } from 'vitest';
import { renderWithTheme } from '@/test/renderWithTheme';
import { NotificationProvider, useNotify } from './NotificationProvider';

function Trigger({ message, severity }: { message: string; severity?: 'success' | 'error' }) {
  const notify = useNotify();
  return <button onClick={() => notify(message, severity)}>notify</button>;
}

describe('NotificationProvider', () => {
  it('announces a confirmation politely', async () => {
    renderWithTheme(
      <NotificationProvider>
        <Trigger message="Coffee added" />
      </NotificationProvider>,
    );

    await userEvent.click(screen.getByRole('button', { name: 'notify' }));

    expect(screen.getByRole('status')).toHaveTextContent('Coffee added');
  });

  it('announces an error as an alert', async () => {
    renderWithTheme(
      <NotificationProvider>
        <Trigger message="Could not save" severity="error" />
      </NotificationProvider>,
    );

    await userEvent.click(screen.getByRole('button', { name: 'notify' }));

    expect(screen.getByRole('alert')).toHaveTextContent('Could not save');
  });

  it('is a no-op without a provider', async () => {
    renderWithTheme(<Trigger message="Coffee added" />);

    await userEvent.click(screen.getByRole('button', { name: 'notify' }));

    expect(screen.queryByText('Coffee added')).not.toBeInTheDocument();
  });
});
