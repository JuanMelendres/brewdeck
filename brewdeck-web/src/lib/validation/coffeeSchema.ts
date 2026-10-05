import { z } from 'zod';
import { vk } from '@/i18n/validationKey';

export const coffeeSchema = z.object({
  name: z
    .string()
    .min(1, vk('coffee.nameRequired'))
    .max(120, vk('coffee.nameTooLong')),
  brand: z.string().max(120, vk('coffee.brandTooLong')).optional(),
  origin: z.string().max(120, vk('coffee.originTooLong')).optional(),
  region: z.string().max(120, vk('coffee.regionTooLong')).optional(),
  farm: z.string().max(120, vk('coffee.farmTooLong')).optional(),
  producer: z.string().max(120, vk('coffee.producerTooLong')).optional(),
  variety: z.string().max(120, vk('coffee.varietyTooLong')).optional(),
  process: z.string().max(80, vk('coffee.processTooLong')).optional(),
  roastLevel: z.string().max(80, vk('coffee.roastLevelTooLong')).optional(),
  notesPrimary: z.string().max(255, vk('coffee.primaryNotesTooLong')).optional(),
  notesSecondary: z.string().max(500, vk('coffee.secondaryNotesTooLong')).optional(),
  acidityScore: z.number().int().min(1).max(5).optional(),
  bodyScore: z.number().int().min(1).max(5).optional(),
  sweetnessScore: z.number().int().min(1).max(5).optional(),
  bitternessScore: z.number().int().min(1).max(5).optional(),
  description: z.string().max(1000, vk('coffee.descriptionTooLong')).optional(),
});

export type CoffeeFormValues = z.infer<typeof coffeeSchema>;
