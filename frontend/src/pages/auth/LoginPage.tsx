import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { Link, useLocation, useNavigate } from 'react-router-dom'
import { z } from 'zod'

import { useAuthStore } from '@/features/auth/model/auth-store'
import { authApi } from '@/shared/api/auth'
import { getErrorMessage } from '@/shared/lib/errors'
import { setStoredTokens } from '@/shared/lib/storage'
import { emailSchema } from '@/shared/lib/validation'
import { Button } from '@/shared/ui/Button'
import { FieldError } from '@/shared/ui/FieldError'
import { Input } from '@/shared/ui/Input'

const loginSchema = z.object({
  email: emailSchema,
  password: z.string().min(1, 'Введите пароль'),
})

type LoginFormValues = z.infer<typeof loginSchema>

export function LoginPage() {
  const navigate = useNavigate()
  const location = useLocation()
  const { setAuthenticated } = useAuthStore()

  const form = useForm<LoginFormValues>({
    resolver: zodResolver(loginSchema),
    defaultValues: {
      email: '',
      password: '',
    },
  })

  const loginMutation = useMutation({
    mutationFn: async (values: LoginFormValues) => {
      const tokens = await authApi.login(values)
      setStoredTokens(tokens)
      return authApi.me()
    },
    onSuccess: (user) => {
      setAuthenticated(user)

      const redirectPath =
        typeof location.state === 'object' &&
        location.state !== null &&
        'from' in location.state &&
        typeof location.state.from === 'object' &&
        location.state.from !== null &&
        'pathname' in location.state.from &&
        typeof location.state.from.pathname === 'string'
          ? location.state.from.pathname
          : '/app/courses'

      navigate(redirectPath, { replace: true })
    },
    onError: () => {
      useAuthStore.getState().resetSession()
    },
  })

  return (
    <div className="flex min-h-screen items-center justify-center px-6 py-10">
      <div className="grid w-full max-w-5xl gap-6 lg:grid-cols-[1.15fr_0.85fr]">
        <section className="rounded-3xl border border-slate-800 bg-slate-950/50 p-8 shadow-2xl shadow-slate-950/40">
          <p className="text-sm uppercase tracking-[0.32em] text-blue-300">
            CodeRoom LMS
          </p>
          <h1 className="mt-4 text-4xl font-semibold text-white">
            Платформа для курсов по программированию
          </h1>
          <p className="mt-4 max-w-2xl text-base text-slate-300">
            Единое пространство с курсами, материалами и заданиями для преподавателя и студента.
          </p>
          <div className="mt-8 grid gap-4 md:grid-cols-3">
            {[
              'Публикация и хранение материалов',
              'Задания и автопроверки',
              'Единое пространство преподавателя и студента',
            ].map((item) => (
              <div
                key={item}
                className="rounded-2xl border border-slate-800 bg-slate-900/70 p-4 text-sm text-slate-300"
              >
                {item}
              </div>
            ))}
          </div>
        </section>

        <section className="rounded-3xl border border-slate-800 bg-slate-950/80 p-8">
          <h2 className="text-2xl font-semibold text-white">Вход</h2>
          <p className="mt-2 text-sm text-slate-400">
            Используйте вашу учетную запись или создайте новую.
          </p>

          <form
            className="mt-8 space-y-5"
            onSubmit={form.handleSubmit((values) => loginMutation.mutate(values))}
          >
            <label className="block space-y-2">
              <span className="text-sm text-slate-300">Email</span>
              <Input
                type="text"
                autoComplete="email"
                placeholder="teacher@coderoom.local"
                {...form.register('email')}
              />
              <FieldError message={form.formState.errors.email?.message} />
            </label>

            <label className="block space-y-2">
              <span className="text-sm text-slate-300">Пароль</span>
              <Input
                type="password"
                autoComplete="current-password"
                placeholder="Минимум 8 символов"
                {...form.register('password')}
              />
              <FieldError message={form.formState.errors.password?.message} />
            </label>

            {loginMutation.isError ? (
              <div className="rounded-2xl border border-rose-500/30 bg-rose-500/10 px-4 py-3 text-sm text-rose-200">
                {getErrorMessage(loginMutation.error, 'Не удалось войти')}
              </div>
            ) : null}

            <Button className="w-full" type="submit" isLoading={loginMutation.isPending}>
              Войти
            </Button>
          </form>

          <p className="mt-6 text-sm text-slate-400">
            Нет аккаунта?{' '}
            <Link className="text-blue-300 hover:text-blue-200" to="/register">
              Зарегистрироваться
            </Link>
          </p>
        </section>
      </div>
    </div>
  )
}
