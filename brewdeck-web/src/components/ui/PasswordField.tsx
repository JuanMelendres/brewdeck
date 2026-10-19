'use client';

import VisibilityOffOutlinedIcon from '@mui/icons-material/VisibilityOffOutlined';
import VisibilityOutlinedIcon from '@mui/icons-material/VisibilityOutlined';
import IconButton from '@mui/material/IconButton';
import InputAdornment from '@mui/material/InputAdornment';
import TextField, { type TextFieldProps } from '@mui/material/TextField';
import { useTranslations } from 'next-intl';
import { useState } from 'react';

/**
 * A password input with a show/hide toggle (NIST SP 800-63B, ADR-018): long passphrases are easier
 * to type correctly when they can be checked. Starts hidden; the toggle is a real button with an
 * accessible name and `aria-pressed`.
 */
export function PasswordField(props: Omit<TextFieldProps, 'type'>) {
  const tc = useTranslations('common');
  const [visible, setVisible] = useState(false);
  const label = visible ? tc('hidePassword') : tc('showPassword');

  return (
    <TextField
      {...props}
      type={visible ? 'text' : 'password'}
      slotProps={{
        ...props.slotProps,
        input: {
          endAdornment: (
            <InputAdornment position="end">
              <IconButton
                aria-label={label}
                aria-pressed={visible}
                edge="end"
                onClick={() => setVisible((value) => !value)}
                // Keep focus (and the caret) in the input while toggling.
                onMouseDown={(event) => event.preventDefault()}
              >
                {visible ? <VisibilityOffOutlinedIcon /> : <VisibilityOutlinedIcon />}
              </IconButton>
            </InputAdornment>
          ),
        },
      }}
    />
  );
}
