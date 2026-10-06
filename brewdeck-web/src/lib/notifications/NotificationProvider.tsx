'use client';

import Alert from '@mui/material/Alert';
import Snackbar from '@mui/material/Snackbar';
import { createContext, useCallback, useContext, useState, type ReactNode } from 'react';

type Severity = 'success' | 'error' | 'info';

type Notification = { id: number; message: string; severity: Severity };

type Notify = (message: string, severity?: Severity) => void;

// Without a provider (isolated component tests) notifications are a no-op.
const NotificationContext = createContext<Notify>(() => {});

/** Short confirmation toasts ("Coffee added") shown at the bottom of the screen. */
export function NotificationProvider({ children }: { children: ReactNode }) {
  const [current, setCurrent] = useState<Notification | null>(null);
  const [open, setOpen] = useState(false);

  const notify = useCallback<Notify>((message, severity = 'success') => {
    // A new id remounts the Snackbar, so back-to-back messages each get their full display time.
    setCurrent({ id: Date.now(), message, severity });
    setOpen(true);
  }, []);

  return (
    <NotificationContext.Provider value={notify}>
      {children}
      <Snackbar
        key={current?.id}
        open={open}
        autoHideDuration={4000}
        onClose={(_event, reason) => {
          if (reason !== 'clickaway') setOpen(false);
        }}
        anchorOrigin={{ vertical: 'bottom', horizontal: 'center' }}
      >
        {current ? (
          <Alert
            severity={current.severity}
            variant="filled"
            onClose={() => setOpen(false)}
            // Errors interrupt; confirmations are announced politely.
            role={current.severity === 'error' ? 'alert' : 'status'}
            sx={{ minWidth: 280, boxShadow: 3 }}
          >
            {current.message}
          </Alert>
        ) : undefined}
      </Snackbar>
    </NotificationContext.Provider>
  );
}

export function useNotify(): Notify {
  return useContext(NotificationContext);
}
