import { z } from 'zod';

export const coffeeSchema = z.object({
  name: z
    .string()
    .min(1, 'Name is required')
    .max(120, 'Name must not exceed 120 characters'),
  brand: z.string().max(120, 'Brand must not exceed 120 characters').optional(),
  origin: z.string().max(120, 'Origin must not exceed 120 characters').optional(),
  region: z.string().max(120, 'Region must not exceed 120 characters').optional(),
  farm: z.string().max(120, 'Farm must not exceed 120 characters').optional(),
  producer: z.string().max(120, 'Producer must not exceed 120 characters').optional(),
  variety: z.string().max(120, 'Variety must not exceed 120 characters').optional(),
  process: z.string().max(80, 'Process must not exceed 80 characters').optional(),
  roastLevel: z.string().max(80, 'Roast level must not exceed 80 characters').optional(),
  notesPrimary: z.string().max(255, 'Primary notes must not exceed 255 characters').optional(),
  notesSecondary: z.string().max(500, 'Secondary notes must not exceed 500 characters').optional(),
  acidityScore: z.number().int().min(1).max(5).optional(),
  bodyScore: z.number().int().min(1).max(5).optional(),
  sweetnessScore: z.number().int().min(1).max(5).optional(),
  bitternessScore: z.number().int().min(1).max(5).optional(),
  description: z.string().max(1000, 'Description must not exceed 1000 characters').optional(),
});

export type CoffeeFormValues = z.infer<typeof coffeeSchema>;
