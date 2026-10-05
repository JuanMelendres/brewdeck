import { z } from 'zod';
import { vk } from '@/i18n/validationKey';

// Mirrors BrewMethodRequest on the backend.
export const brewMethodSchema = z.object({
  name: z
    .string()
    .trim()
    .min(1, vk('brewMethod.nameRequired'))
    .max(80, vk('brewMethod.nameTooLong')),
  description: z.string().max(500, vk('brewMethod.descriptionTooLong')).optional(),
});

export type BrewMethodFormValues = z.infer<typeof brewMethodSchema>;
