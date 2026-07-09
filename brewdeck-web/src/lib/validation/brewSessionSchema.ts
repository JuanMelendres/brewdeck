import { z } from 'zod';

function optionalNumber<T extends z.ZodTypeAny>(inner: T) {
  return z.preprocess(
    (value) => (value === '' || value === null || value === undefined ? undefined : value),
    inner.optional(),
  );
}

export const brewSessionSchema = z.object({
  recipeId: z.coerce.number().int().positive('Recipe is required'),
  actualGrind: z.string().max(120, 'Actual grind must not exceed 120 characters').optional(),
  actualTemp: optionalNumber(
    z.coerce
      .number()
      .min(70, 'Actual temperature must be at least 70 degrees Celsius')
      .max(100, 'Actual temperature must not exceed 100 degrees Celsius'),
  ),
  actualTime: z.string().max(20, 'Actual time must not exceed 20 characters').optional(),
  tasteResult: z.string().max(1000, 'Taste result must not exceed 1000 characters').optional(),
  rating: optionalNumber(
    z.coerce
      .number()
      .int()
      .min(1, 'Rating must be at least 1')
      .max(10, 'Rating must not exceed 10'),
  ),
  adjustmentNotes: z.string().max(1000, 'Adjustment notes must not exceed 1000 characters').optional(),
});

export type BrewSessionFormValues = z.infer<typeof brewSessionSchema>;
