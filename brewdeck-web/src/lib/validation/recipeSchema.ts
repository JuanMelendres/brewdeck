import { z } from 'zod';
import { vk } from '@/i18n/validationKey';

function optionalNumber<T extends z.ZodTypeAny>(inner: T) {
  return z.preprocess(
    (value) => (value === '' || value === null || value === undefined ? undefined : value),
    inner.optional(),
  );
}

/** True when `value` has at most `places` decimals; tolerant of float noise (0.1 + 0.2). */
function hasAtMostDecimals(value: number, places: number): boolean {
  const scaled = value * 10 ** places;
  return Math.abs(scaled - Math.round(scaled)) < 1e-6;
}

/** Temperatures are NUMERIC(4,1): 92.5 yes, 92.75 no. */
export function hasAtMostOneDecimal(value: number): boolean {
  return hasAtMostDecimals(value, 1);
}

/** Grams are DECIMAL(6,2): 15.13 yes, 15.126 no. */
export function hasAtMostTwoDecimals(value: number): boolean {
  return hasAtMostDecimals(value, 2);
}

export const recipeSchema = z.object({
  coffeeId: z.coerce.number().int().positive(vk('recipe.coffeeRequired')),
  methodId: z.coerce.number().int().positive(vk('recipe.methodRequired')),
  name: z.string().min(1, vk('recipe.nameRequired')).max(120, vk('recipe.nameTooLong')),
  // Backend stores grams as DECIMAL(6,2): at most 9999.99 and two decimals.
  coffeeGrams: optionalNumber(
    z.coerce
      .number()
      .positive(vk('recipe.coffeeGramsPositive'))
      .max(9999.99, vk('recipe.coffeeGramsTooHigh'))
      .refine(hasAtMostTwoDecimals, vk('recipe.coffeeGramsTwoDecimals')),
  ),
  waterGrams: optionalNumber(
    z.coerce
      .number()
      .positive(vk('recipe.waterGramsPositive'))
      .max(9999.99, vk('recipe.waterGramsTooHigh'))
      .refine(hasAtMostTwoDecimals, vk('recipe.waterGramsTwoDecimals')),
  ),
  ratio: z.string().max(20, vk('recipe.ratioTooLong')).optional(),
  grindSetting: z.string().max(120, vk('recipe.grindSettingTooLong')).optional(),
  waterTemp: optionalNumber(
    z.coerce
      .number()
      .min(0, vk('recipe.waterTempTooLow'))
      .max(100, vk('recipe.waterTempTooHigh'))
      // Stored as NUMERIC(4,1): one decimal, e.g. 92.5.
      .refine(hasAtMostOneDecimal, vk('recipe.waterTempOneDecimal')),
  ),
  brewTime: z.string().max(20, vk('recipe.brewTimeTooLong')).optional(),
  steps: z.string().max(1000, vk('recipe.stepsTooLong')).optional(),
  expectedTaste: z.string().max(500, vk('recipe.expectedTasteTooLong')).optional(),
  favorite: z.boolean().optional(),
});

export type RecipeFormValues = z.infer<typeof recipeSchema>;
