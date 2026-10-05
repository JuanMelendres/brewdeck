import Divider from '@mui/material/Divider';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';
import { useTranslations } from 'next-intl';
import { ChangePasswordForm } from '@/components/auth/ChangePasswordForm';
import { ProfileForm } from '@/components/auth/ProfileForm';
import { ThemeModeSetting } from '@/components/theme/ThemeModeSetting';

export default function AccountPage() {
  const t = useTranslations('nav');
  return (
    <Stack spacing={4} sx={{ maxWidth: 480 }}>
      <Typography variant="h5" component="h1">
        {t('account')}
      </Typography>
      <ProfileForm />
      <Divider />
      <ThemeModeSetting />
      <Divider />
      <ChangePasswordForm />
    </Stack>
  );
}
