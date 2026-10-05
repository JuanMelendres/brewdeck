import { z } from 'zod';
import { vk } from '@/i18n/validationKey';

function optionalNumber<T extends z.ZodTypeAny>(inner: T) {
  return z.preprocess(
    (value) => (value === '' || value === null || value === undefined ? undefined : value),
    inner.optional(),
  );
}

export const recipeSchema = z.object({
  coffeeId: z.coerce.number().int().positive(vk('recipe.coffeeRequired')),
  methodId: z.coerce.number().int().positive(vk('recipe.methodRequired')),
  name: z.string().min(1, vk('recipe.nameRequired')).max(120, vk('recipe.nameTooLong')),
  // Backend stores grams as DECIMAL(6,2): at most 9999.99.
  coffeeGrams: optionalNumber(
    z.coerce
      .number()
      .positive(vk('recipe.coffeeGramsPositive'))
      .max(9999.99, vk('recipe.coffeeGramsTooHigh')),
  ),
  waterGrams: optionalNumber(
    z.coerce
      .number()
      .positive(vk('recipe.waterGramsPositive'))
      .max(9999.99, vk('recipe.waterGramsTooHigh')),
  ),
  ratio: z.string().max(20, vk('recipe.ratioTooLong')).optional(),
  grindSetting: z.string().max(120, vk('recipe.grindSettingTooLong')).optional(),
  waterTemp: optionalNumber(
    z.coerce
      .number()
      .min(70, vk('recipe.waterTempTooLow'))
      .max(100, vk('recipe.waterTempTooHigh')),
  ),
  brewTime: z.string().max(20, vk('recipe.brewTimeTooLong')).optional(),
  steps: z.string().max(1000, vk('recipe.stepsTooLong')).optional(),
  expectedTaste: z.string().max(500, vk('recipe.expectedTasteTooLong')).optional(),
  favorite: z.boolean().optional(),
});

export type RecipeFormValues = z.infer<typeof recipeSchema>;
