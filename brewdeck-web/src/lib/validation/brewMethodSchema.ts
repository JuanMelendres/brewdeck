import { z } from 'zod';

// Mirrors BrewMethodRequest on the backend.
export const brewMethodSchema = z.object({
  name: z
    .string()
    .trim()
    .min(1, 'Name is required')
    .max(80, 'Name must not exceed 80 characters'),
  description: z.string().max(500, 'Description must not exceed 500 characters').optional(),
});

export type BrewMethodFormValues = z.infer<typeof brewMethodSchema>;
