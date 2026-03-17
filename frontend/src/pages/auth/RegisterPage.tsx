import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { Link, useNavigate } from 'react-router-dom'
import { z } from 'zod'

import { useAuthStore } from '@/features/auth/model/auth-store'
import { authApi } from '@/shared/api/auth'
import { getErrorMessage } from '@/shared/lib/errors'
import { setStoredTokens } from '@/shared/lib/storage'
import { emailSchema } from '@/shared/lib/validation'
import { Button } from '@/shared/ui/Button'
import { FieldError } from '@/shared/ui/FieldError'
import { Input } from '@/shared/ui/Input'

const registerSchema = z
  .object({
    email: emailSchema,
    password: z
      .string()
      .min(8, 'Пароль должен содержать минимум 8 символов')
      .max(128, 'Слишком длинный пароль'),
    confirmPassword: z.string().min(8, 'Повторите пароль'),
  })
  .refine((values) => values.password === values.confirmPassword, {
    message: 'Пароли должны совпадать',
    path: ['confirmPassword'],
  })

type RegisterFormValues = z.infer<typeof registerSchema>

export function RegisterPage() {
  const navigate = useNavigate()
  const { setAuthenticated, setBootstrapping } = useAuthStore()

  const form = useForm<RegisterFormValues>({
    resolver: zodResolver(registerSchema),
    defaultValues: {
      email: '',
      password: '',
      confirmPassword: '',
    },
  })

  const registerMutation = useMutation({
    mutationFn: async (values: RegisterFormValues) => {
      const tokens = await authApi.register({
        email: values.email,
        password: values.password,
      })
      setStoredTokens(tokens)
      return authApi.me()
    },
    onMutate: () => {
      setBootstrapping()
    },
    onSuccess: (user) => {
      setAuthenticated(user)
      navigate('/app/courses', { replace: true })
    },
    onError: () => {
      useAuthStore.getState().resetSession()
    },
  })

  return (
    <div className="flex min-h-screen items-center justify-center px-6 py-10">
      <section className="w-full max-w-xl rounded-3xl border border-slate-800 bg-slate-950/80 p-8">
        <p className="text-sm uppercase tracking-[0.32em] text-blue-300">CodeRoom LMS</p>
        <h1 className="mt-4 text-3xl font-semibold text-white">
          Регистрация пользователя
        </h1>
        <p className="mt-2 text-sm text-slate-400">
          Новый пользователь создается со стартовой ролью студента.
        </p>

        <form
          className="mt-8 space-y-5"
          onSubmit={form.handleSubmit((values) => registerMutation.mutate(values))}
        >
          <label className="block space-y-2">
            <span className="text-sm text-slate-300">Email</span>
            <Input type="text" autoComplete="email" {...form.register('email')} />
            <FieldError message={form.formState.errors.email?.message} />
          </label>

          <label className="block space-y-2">
            <span className="text-sm text-slate-300">Пароль</span>
            <Input
              type="password"
              autoComplete="new-password"
              {...form.register('password')}
            />
            <FieldError message={form.formState.errors.password?.message} />
          </label>

          <label className="block space-y-2">
            <span className="text-sm text-slate-300">Повторите пароль</span>
            <Input
              type="password"
              autoComplete="new-password"
              {...form.register('confirmPassword')}
            />
            <FieldError message={form.formState.errors.confirmPassword?.message} />
          </label>

          {registerMutation.isError ? (
            <div className="rounded-2xl border border-rose-500/30 bg-rose-500/10 px-4 py-3 text-sm text-rose-200">
              {getErrorMessage(registerMutation.error, 'Не удалось зарегистрироваться')}
            </div>
          ) : null}

          <Button className="w-full" type="submit" isLoading={registerMutation.isPending}>
            Создать аккаунт
          </Button>
        </form>

        <p className="mt-6 text-sm text-slate-400">
          Уже есть аккаунт?{' '}
          <Link className="text-blue-300 hover:text-blue-200" to="/login">
            Вернуться ко входу
          </Link>
        </p>
      </section>
    </div>
  )
}
