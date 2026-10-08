import { z } from 'zod';
import { vk } from '@/i18n/validationKey';

function optionalNumber<T extends z.ZodTypeAny>(inner: T) {
  return z.preprocess(
    (value) => (value === '' || value === null || value === undefined ? undefined : value),
    inner.optional(),
  );
}

export const brewSessionSchema = z.object({
  recipeId: z.coerce.number().int().positive(vk('brewSession.recipeRequired')),
  actualGrind: z.string().max(120, vk('brewSession.actualGrindTooLong')).optional(),
  actualTemp: optionalNumber(
    z.coerce
      .number()
      .min(0, vk('brewSession.actualTempTooLow'))
      .max(100, vk('brewSession.actualTempTooHigh')),
  ),
  actualTime: z.string().max(20, vk('brewSession.actualTimeTooLong')).optional(),
  tasteResult: z.string().max(1000, vk('brewSession.tasteResultTooLong')).optional(),
  rating: optionalNumber(
    z.coerce
      .number()
      .int()
      .min(1, vk('brewSession.ratingTooLow'))
      .max(10, vk('brewSession.ratingTooHigh')),
  ),
  adjustmentNotes: z.string().max(1000, vk('brewSession.adjustmentNotesTooLong')).optional(),
});

export type BrewSessionFormValues = z.infer<typeof brewSessionSchema>;
