import type en from '../../messages/en.json';

type Leaves<T, Prefix extends string = ''> = {
  [K in keyof T & string]: T[K] extends string ? `${Prefix}${K}` : Leaves<T[K], `${Prefix}${K}.`>;
}[keyof T & string];

/** A message key under `validation` in messages/en.json, e.g. 'emailRequired' or 'coffee.nameTooLong'. */
export type ValidationKey = Leaves<(typeof en)['validation']>;

/**
 * Marks a Zod message as a translation key, so a misspelled key fails `pnpm type-check`.
 * Forms translate it with `useFieldError()`.
 */
export function vk(key: ValidationKey): ValidationKey {
  return key;
}
