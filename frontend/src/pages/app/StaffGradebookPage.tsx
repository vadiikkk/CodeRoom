import { useQuery } from '@tanstack/react-query'
import { useMemo } from 'react'
import { Link, useParams } from 'react-router-dom'

import { authApi } from '@/shared/api/auth'
import { coursesApi } from '@/shared/api/courses'
import { getErrorMessage } from '@/shared/lib/errors'
import { getStoredTokens } from '@/shared/lib/storage'
import { env } from '@/shared/config/env'
import { Button } from '@/shared/ui/Button'
import { EmptyState } from '@/shared/ui/EmptyState'
import { PageLoader } from '@/shared/ui/PageLoader'

export function StaffGradebookPage() {
  const params = useParams<{ courseId: string }>()
  const courseId = params.courseId

  const courseQuery = useQuery({
    queryKey: ['course', courseId],
    queryFn: () => coursesApi.getCourse(courseId!),
    enabled: Boolean(courseId),
  })

  const membershipQuery = useQuery({
    queryKey: ['course-membership', courseId],
    queryFn: () => coursesApi.getMembership(courseId!),
    enabled: Boolean(courseId),
  })

  const gradebookQuery = useQuery({
    queryKey: ['gradebook', courseId],
    queryFn: () => coursesApi.getGradebook(courseId!),
    enabled: Boolean(courseId),
  })

  const userIds = useMemo(
    () => gradebookQuery.data?.rows.map((row) => row.userId) ?? [],
    [gradebookQuery.data],
  )

  const usersQuery = useQuery({
    queryKey: ['users-lookup', userIds],
    queryFn: () => authApi.lookupByIds({ userIds }),
    enabled: userIds.length > 0,
  })

  const emailMap = useMemo(() => {
    const map = new Map<string, string>()
    for (const user of usersQuery.data?.users ?? []) {
      map.set(user.userId, user.email)
    }
    return map
  }, [usersQuery.data])

  if (!courseId) {
    return <EmptyState title="Не найдено" description="Отсутствует идентификатор курса." />
  }

  if (courseQuery.isPending || membershipQuery.isPending || gradebookQuery.isPending) {
    return <PageLoader label="Загружаем ведомость..." />
  }

  const firstError = [courseQuery.error, membershipQuery.error, gradebookQuery.error].find(Boolean)
  if (firstError) {
    return <EmptyState title="Ошибка" description={getErrorMessage(firstError)} />
  }

  const membership = membershipQuery.data!
  if (membership.roleInCourse === 'STUDENT') {
    return (
      <EmptyState
        title="Доступ ограничен"
        description="Ведомость преподавателя доступна только для преподавателей и ассистентов."
      />
    )
  }

  const gradebook = gradebookQuery.data!
  const assignments = gradebook.assignments
  const rows = gradebook.rows

  function handleExportCsv() {
    const tokens = getStoredTokens()
    if (!tokens) return
    const url = `${env.apiBaseUrl}${coursesApi.getGradebookCsvUrl(courseId!)}`
    const anchor = document.createElement('a')
    anchor.href = url
    anchor.download = `gradebook-${courseId}.csv`

    const headers = new Headers()
    headers.set('Authorization', `${tokens.tokenType} ${tokens.accessToken}`)
    fetch(url, { headers })
      .then((res) => res.blob())
      .then((blob) => {
        const blobUrl = URL.createObjectURL(blob)
        anchor.href = blobUrl
        anchor.click()
        URL.revokeObjectURL(blobUrl)
      })
  }

  return (
    <div className="space-y-8">
      <section className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <div className="text-sm text-slate-400">
            <Link
              to={`/app/courses/${courseId}`}
              className="text-blue-400 transition hover:text-blue-300"
            >
              {courseQuery.data!.title}
            </Link>
            {' / '}
            <Link
              to={`/app/courses/${courseId}/manage`}
              className="text-blue-400 transition hover:text-blue-300"
            >
              Управление
            </Link>
          </div>
          <h1 className="mt-2 text-2xl font-bold text-white">Ведомость</h1>
        </div>
        <Button variant="secondary" onClick={handleExportCsv}>
          Экспорт CSV
        </Button>
      </section>

      {rows.length === 0 ? (
        <EmptyState title="Нет данных" description="Пока ни один студент не сдал работу." />
      ) : (
        <div className="overflow-x-auto rounded-2xl border border-slate-800">
          <table className="w-full text-left text-sm">
            <thead>
              <tr className="border-b border-slate-800 bg-slate-900/70">
                <th className="sticky left-0 z-10 bg-slate-900/70 px-4 py-3 font-medium text-slate-300">
                  Студент
                </th>
                {assignments.map((a) => (
                  <th key={a.assignmentId} className="px-4 py-3 font-medium text-slate-300">
                    <div className="max-w-[160px] truncate" title={a.title}>
                      {a.title}
                    </div>
                    <div className="mt-0.5 text-xs font-normal text-slate-500">
                      вес {a.weight}
                    </div>
                  </th>
                ))}
                <th className="px-4 py-3 font-medium text-slate-300">Итог</th>
              </tr>
            </thead>
            <tbody>
              {rows
                .slice()
                .sort((a, b) => b.totalWeightedScore - a.totalWeightedScore)
                .map((row) => {
                  const entryMap = new Map(row.entries.map((e) => [e.assignmentId, e]))
                  return (
                    <tr key={row.userId} className="border-b border-slate-800/60 hover:bg-slate-900/40">
                      <td className="sticky left-0 z-10 bg-slate-950/70 px-4 py-3 font-medium text-white">
                        {emailMap.get(row.userId) ?? row.userId.slice(0, 8)}
                      </td>
                      {assignments.map((a) => {
                        const entry = entryMap.get(a.assignmentId)
                        return (
                          <td key={a.assignmentId} className="px-4 py-3 text-slate-300">
                            {entry?.score != null ? (
                              <span>{entry.score}</span>
                            ) : entry?.status ? (
                              <span className="text-amber-300">{entry.status === 'SUBMITTED' ? 'Сдано' : entry.status}</span>
                            ) : (
                              <span className="text-slate-600">—</span>
                            )}
                          </td>
                        )
                      })}
                      <td className="px-4 py-3 font-semibold text-white">
                        {row.totalWeightedScore.toFixed(1)}
                      </td>
                    </tr>
                  )
                })}
            </tbody>
          </table>
        </div>
      )}
    </div>
  )
}
