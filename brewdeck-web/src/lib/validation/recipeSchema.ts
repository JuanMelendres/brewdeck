import { z } from 'zod';

function optionalNumber<T extends z.ZodTypeAny>(inner: T) {
  return z.preprocess(
    (value) => (value === '' || value === null || value === undefined ? undefined : value),
    inner.optional(),
  );
}

export const recipeSchema = z.object({
  coffeeId: z.coerce.number().int().positive('Coffee is required'),
  methodId: z.coerce.number().int().positive('Brew method is required'),
  name: z.string().min(1, 'Name is required').max(120, 'Name must not exceed 120 characters'),
  coffeeGrams: optionalNumber(z.coerce.number().positive('Coffee grams must be greater than zero')),
  waterGrams: optionalNumber(z.coerce.number().positive('Water grams must be greater than zero')),
  ratio: z.string().max(20, 'Ratio must not exceed 20 characters').optional(),
  grindSetting: z.string().max(120, 'Grind setting must not exceed 120 characters').optional(),
  waterTemp: optionalNumber(
    z.coerce
      .number()
      .min(70, 'Water temperature must be at least 70 degrees Celsius')
      .max(100, 'Water temperature must not exceed 100 degrees Celsius'),
  ),
  brewTime: z.string().max(20, 'Brew time must not exceed 20 characters').optional(),
  steps: z.string().max(1000, 'Steps must not exceed 1000 characters').optional(),
  expectedTaste: z.string().max(500, 'Expected taste must not exceed 500 characters').optional(),
  favorite: z.boolean().optional(),
});

export type RecipeFormValues = z.infer<typeof recipeSchema>;
