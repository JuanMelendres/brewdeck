import Divider from '@mui/material/Divider';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';
import { ChangePasswordForm } from '@/components/auth/ChangePasswordForm';
import { ProfileForm } from '@/components/auth/ProfileForm';

export default function AccountPage() {
  return (
    <Stack spacing={4} sx={{ maxWidth: 480 }}>
      <Typography variant="h5" component="h1">
        Account
      </Typography>
      <ProfileForm />
      <Divider />
      <ChangePasswordForm />
    </Stack>
  );
}
