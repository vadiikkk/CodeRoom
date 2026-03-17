import { z } from 'zod'

export const emailSchema = z
  .string()
  .trim()
  .min(1, 'Введите email')
  .refine((value) => value.includes('@') && value.indexOf('@') > 0, {
    message: 'Введите корректный email',
  })
