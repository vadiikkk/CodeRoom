import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'

import type { AdminUserResponse } from '@/entities/auth/types'
import type { GlobalRole } from '@/entities/auth/types'
import { useAuthStore } from '@/features/auth/model/auth-store'
import { authApi } from '@/shared/api/auth'
import { getErrorMessage } from '@/shared/lib/errors'
import { formatDateTime } from '@/shared/lib/format'
import { Button } from '@/shared/ui/Button'
import { EmptyState } from '@/shared/ui/EmptyState'
import { Input } from '@/shared/ui/Input'
import { PageLoader } from '@/shared/ui/PageLoader'

export function AdminPage() {
  const { currentUser } = useAuthStore()
  const queryClient = useQueryClient()
  const [searchQuery, setSearchQuery] = useState('')
  const [appliedQuery, setAppliedQuery] = useState('')
  const [roleEditTarget, setRoleEditTarget] = useState<{ email: string; role: GlobalRole } | null>(null)

  const usersQuery = useQuery({
    queryKey: ['admin-users', appliedQuery],
    queryFn: () => authApi.adminListUsers(appliedQuery, 0, 100),
  })

  const setRoleMutation = useMutation({
    mutationFn: (payload: { email: string; role: GlobalRole }) => authApi.adminSetRole(payload),
    onSuccess: () => {
      setRoleEditTarget(null)
      queryClient.invalidateQueries({ queryKey: ['admin-users'] })
    },
  })

  const setActiveMutation = useMutation({
    mutationFn: (payload: { email: string; isActive: boolean }) => authApi.adminSetActive(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-users'] })
    },
  })

  if (currentUser?.role !== 'TEACHER') {
    return (
      <EmptyState
        title="Доступ ограничен"
        description="Панель администратора доступна только для преподавателей."
      />
    )
  }

  return (
    <div className="space-y-8">
      <section>
        <h1 className="text-2xl font-bold text-white">Администрирование</h1>
        <p className="mt-1 text-sm text-slate-400">
          Поиск пользователей, смена ролей и управление активностью аккаунтов.
        </p>
      </section>

      <section className="flex flex-wrap items-end gap-3">
        <div className="flex-1">
          <Input
            placeholder="Поиск по email..."
            value={searchQuery}
            onChange={(event) => setSearchQuery(event.target.value)}
            onKeyDown={(event) => {
              if (event.key === 'Enter') setAppliedQuery(searchQuery.trim())
            }}
          />
        </div>
        <Button onClick={() => setAppliedQuery(searchQuery.trim())}>Найти</Button>
      </section>

      {usersQuery.isPending ? (
        <PageLoader label="Загружаем пользователей..." />
      ) : usersQuery.isError ? (
        <EmptyState title="Ошибка" description={getErrorMessage(usersQuery.error)} />
      ) : usersQuery.data!.length === 0 ? (
        <EmptyState title="Не найдено" description="Пользователи не найдены." />
      ) : (
        <div className="space-y-3">
          {usersQuery.data!.map((user) => (
            <UserRow
              key={user.userId}
              user={user}
              roleEditTarget={roleEditTarget}
              setRoleEditTarget={setRoleEditTarget}
              setRoleMutation={setRoleMutation}
              setActiveMutation={setActiveMutation}
            />
          ))}
        </div>
      )}
    </div>
  )
}

function UserRow({
  user,
  roleEditTarget,
  setRoleEditTarget,
  setRoleMutation,
  setActiveMutation,
}: {
  user: AdminUserResponse
  roleEditTarget: { email: string; role: GlobalRole } | null
  setRoleEditTarget: (target: { email: string; role: GlobalRole } | null) => void
  setRoleMutation: ReturnType<typeof useMutation<void, Error, { email: string; role: GlobalRole }>>
  setActiveMutation: ReturnType<typeof useMutation<void, Error, { email: string; isActive: boolean }>>
}) {
  const isEditingRole = roleEditTarget?.email === user.email

  return (
    <article className="rounded-2xl border border-slate-800 bg-slate-950/70 p-5">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <div className="flex items-center gap-2">
            <span className="text-sm font-semibold text-white">{user.email}</span>
            {user.isRoot ? (
              <span className="rounded-full border border-purple-500/30 px-2 py-0.5 text-[10px] font-bold uppercase text-purple-300">
                root
              </span>
            ) : null}
          </div>
          <div className="mt-1 text-xs text-slate-500">
            {getRoleLabel(user.role)} · {user.isActive ? 'Активен' : 'Деактивирован'} ·{' '}
            {formatDateTime(user.createdAt)}
          </div>
        </div>

        <div className="flex items-center gap-2">
          <span
            className={`rounded-full border px-3 py-1 text-xs font-medium ${
              user.isActive
                ? 'border-green-500/30 text-green-300'
                : 'border-rose-500/30 text-rose-300'
            }`}
          >
            {user.isActive ? 'Активен' : 'Неактивен'}
          </span>
        </div>
      </div>

      <div className="mt-4 flex flex-wrap gap-2">
        {isEditingRole ? (
          <div className="flex flex-wrap items-center gap-2">
            {(['STUDENT', 'ASSISTANT', 'TEACHER'] as const).map((role) => (
              <Button
                key={role}
                variant={roleEditTarget.role === role ? 'primary' : 'secondary'}
                onClick={() => setRoleEditTarget({ email: user.email, role })}
              >
                {getRoleLabel(role)}
              </Button>
            ))}
            <Button
              onClick={() => setRoleMutation.mutate(roleEditTarget)}
              isLoading={setRoleMutation.isPending}
            >
              Сохранить
            </Button>
            <Button variant="ghost" onClick={() => setRoleEditTarget(null)}>
              Отмена
            </Button>
          </div>
        ) : (
          <Button
            variant="secondary"
            onClick={() => setRoleEditTarget({ email: user.email, role: user.role })}
          >
            Сменить роль
          </Button>
        )}

        <Button
          variant="secondary"
          onClick={() =>
            setActiveMutation.mutate({ email: user.email, isActive: !user.isActive })
          }
          isLoading={setActiveMutation.isPending}
        >
          {user.isActive ? 'Деактивировать' : 'Активировать'}
        </Button>
      </div>

      {setRoleMutation.isError && isEditingRole ? (
        <div className="mt-3 rounded-2xl border border-rose-500/30 bg-rose-500/10 px-4 py-3 text-sm text-rose-200">
          {getErrorMessage(setRoleMutation.error)}
        </div>
      ) : null}

      {setActiveMutation.isError ? (
        <div className="mt-3 rounded-2xl border border-rose-500/30 bg-rose-500/10 px-4 py-3 text-sm text-rose-200">
          {getErrorMessage(setActiveMutation.error)}
        </div>
      ) : null}
    </article>
  )
}

function getRoleLabel(role: GlobalRole) {
  switch (role) {
    case 'STUDENT':
      return 'Студент'
    case 'ASSISTANT':
      return 'Ассистент'
    case 'TEACHER':
      return 'Преподаватель'
  }
}
