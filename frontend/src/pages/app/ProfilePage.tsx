import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { useNavigate } from 'react-router-dom'
import { z } from 'zod'

import { useAuthStore } from '@/features/auth/model/auth-store'
import { authApi } from '@/shared/api/auth'
import { getErrorMessage } from '@/shared/lib/errors'
import { Button } from '@/shared/ui/Button'
import { FieldError } from '@/shared/ui/FieldError'
import { Input } from '@/shared/ui/Input'

const passwordSchema = z
  .object({
    oldPassword: z.string().min(1, 'Введите текущий пароль'),
    newPassword: z.string().min(8, 'Минимум 8 символов').max(128, 'Максимум 128 символов'),
    confirmPassword: z.string().min(1, 'Подтвердите пароль'),
  })
  .refine((data) => data.newPassword === data.confirmPassword, {
    message: 'Пароли не совпадают',
    path: ['confirmPassword'],
  })

export function ProfilePage() {
  const navigate = useNavigate()
  const { currentUser, resetSession } = useAuthStore()

  const form = useForm({
    resolver: zodResolver(passwordSchema),
    defaultValues: { oldPassword: '', newPassword: '', confirmPassword: '' },
  })

  const changePasswordMutation = useMutation({
    mutationFn: (values: z.infer<typeof passwordSchema>) =>
      authApi.changePassword(values.oldPassword, values.newPassword),
    onSuccess: () => form.reset(),
  })

  const logoutAllMutation = useMutation({
    mutationFn: () => authApi.logoutAll(),
    onSuccess: () => {
      resetSession()
      navigate('/login', { replace: true })
    },
  })

  return (
    <div className="space-y-8">
      <section>
        <h1 className="text-2xl font-bold text-white">Профиль</h1>
        <p className="mt-1 text-sm text-slate-400">
          Управление аккаунтом и безопасностью.
        </p>
      </section>

      <section className="rounded-3xl border border-slate-800 bg-slate-950/70 p-6">
        <h2 className="text-lg font-semibold text-white">Информация об аккаунте</h2>
        <div className="mt-4 grid gap-3 md:grid-cols-2">
          <div className="rounded-2xl border border-slate-800 bg-slate-900/70 p-4">
            <div className="text-xs uppercase tracking-wide text-slate-500">Email</div>
            <div className="mt-2 text-sm font-medium text-white">{currentUser?.email}</div>
          </div>
          <div className="rounded-2xl border border-slate-800 bg-slate-900/70 p-4">
            <div className="text-xs uppercase tracking-wide text-slate-500">Роль</div>
            <div className="mt-2 text-sm font-medium text-white">
              {getRoleLabel(currentUser?.role ?? 'STUDENT')}
            </div>
          </div>
        </div>
      </section>

      <section className="rounded-3xl border border-slate-800 bg-slate-950/70 p-6">
        <h2 className="text-lg font-semibold text-white">Смена пароля</h2>
        <form
          className="mt-4 space-y-4"
          onSubmit={form.handleSubmit((values) => changePasswordMutation.mutate(values))}
        >
          <label className="block space-y-2">
            <span className="text-sm text-slate-300">Текущий пароль</span>
            <Input type="password" {...form.register('oldPassword')} />
            <FieldError message={form.formState.errors.oldPassword?.message} />
          </label>
          <label className="block space-y-2">
            <span className="text-sm text-slate-300">Новый пароль</span>
            <Input type="password" {...form.register('newPassword')} />
            <FieldError message={form.formState.errors.newPassword?.message} />
          </label>
          <label className="block space-y-2">
            <span className="text-sm text-slate-300">Подтвердите новый пароль</span>
            <Input type="password" {...form.register('confirmPassword')} />
            <FieldError message={form.formState.errors.confirmPassword?.message} />
          </label>

          {changePasswordMutation.isError ? (
            <div className="rounded-2xl border border-rose-500/30 bg-rose-500/10 px-4 py-3 text-sm text-rose-200">
              {getErrorMessage(changePasswordMutation.error, 'Не удалось сменить пароль')}
            </div>
          ) : null}

          {changePasswordMutation.isSuccess ? (
            <div className="rounded-2xl border border-green-500/30 bg-green-500/10 px-4 py-3 text-sm text-green-200">
              Пароль успешно изменён.
            </div>
          ) : null}

          <Button type="submit" isLoading={changePasswordMutation.isPending}>
            Сменить пароль
          </Button>
        </form>
      </section>

      <section className="rounded-3xl border border-slate-800 bg-slate-950/70 p-6">
        <h2 className="text-lg font-semibold text-white">Безопасность</h2>
        <p className="mt-2 text-sm text-slate-400">
          Завершить все активные сессии. После этого потребуется повторный вход на всех устройствах.
        </p>
        {logoutAllMutation.isError ? (
          <div className="mt-3 rounded-2xl border border-rose-500/30 bg-rose-500/10 px-4 py-3 text-sm text-rose-200">
            {getErrorMessage(logoutAllMutation.error, 'Не удалось завершить сессии')}
          </div>
        ) : null}
        <div className="mt-4">
          <Button
            variant="secondary"
            onClick={() => logoutAllMutation.mutate()}
            isLoading={logoutAllMutation.isPending}
          >
            Завершить все сессии
          </Button>
        </div>
      </section>
    </div>
  )
}

function getRoleLabel(role: string) {
  switch (role) {
    case 'STUDENT': return 'Студент'
    case 'ASSISTANT': return 'Ассистент'
    case 'TEACHER': return 'Преподаватель'
    default: return role
  }
}
