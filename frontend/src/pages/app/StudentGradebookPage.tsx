import { useQuery } from '@tanstack/react-query'
import { Link, useParams } from 'react-router-dom'

import { coursesApi } from '@/shared/api/courses'
import { getErrorMessage } from '@/shared/lib/errors'
import { formatDateTime } from '@/shared/lib/format'
import { EmptyState } from '@/shared/ui/EmptyState'
import { PageLoader } from '@/shared/ui/PageLoader'

export function StudentGradebookPage() {
  const params = useParams<{ courseId: string }>()
  const courseId = params.courseId

  const courseQuery = useQuery({
    queryKey: ['course', courseId],
    queryFn: () => coursesApi.getCourse(courseId!),
    enabled: Boolean(courseId),
  })

  const gradebookQuery = useQuery({
    queryKey: ['my-gradebook', courseId],
    queryFn: () => coursesApi.getMyGradebook(courseId!),
    enabled: Boolean(courseId),
  })

  if (!courseId) {
    return <EmptyState title="Не найдено" description="Отсутствует идентификатор курса." />
  }

  if (courseQuery.isPending || gradebookQuery.isPending) {
    return <PageLoader label="Загружаем ваши оценки..." />
  }

  const firstError = [courseQuery.error, gradebookQuery.error].find(Boolean)
  if (firstError) {
    return <EmptyState title="Ошибка" description={getErrorMessage(firstError)} />
  }

  const gradebook = gradebookQuery.data!
  const assignments = gradebook.assignments
  const entryMap = new Map(gradebook.entries.map((e) => [e.assignmentId, e]))

  return (
    <div className="space-y-8">
      <section>
        <div className="text-sm text-slate-400">
          <Link
            to={`/app/courses/${courseId}`}
            className="text-blue-400 transition hover:text-blue-300"
          >
            {courseQuery.data!.title}
          </Link>
        </div>
        <h1 className="mt-2 text-2xl font-bold text-white">Мои оценки</h1>
        <p className="mt-1 text-sm text-slate-400">
          Итоговый балл: <span className="font-semibold text-white">{gradebook.totalWeightedScore.toFixed(1)}</span>
        </p>
      </section>

      {assignments.length === 0 ? (
        <EmptyState title="Нет заданий" description="В курсе пока нет заданий с оценками." />
      ) : (
        <div className="space-y-4">
          {assignments.map((a) => {
            const entry = entryMap.get(a.assignmentId)
            return (
              <article
                key={a.assignmentId}
                className="rounded-2xl border border-slate-800 bg-slate-950/70 p-5"
              >
                <div className="flex flex-wrap items-start justify-between gap-4">
                  <div>
                    <Link
                      to={`/app/courses/${courseId}/assignments/${a.assignmentId}`}
                      className="text-sm font-semibold text-white transition hover:text-blue-300"
                    >
                      {a.title}
                    </Link>
                    <div className="mt-1 text-xs text-slate-500">
                      {getAssignmentTypeLabel(a.assignmentType)} · вес {a.weight}
                    </div>
                  </div>

                  {entry?.score != null ? (
                    <div className="text-right">
                      <div className="text-lg font-bold text-white">{entry.score}</div>
                      <div className="text-xs text-slate-500">
                        взвеш. {entry.weightedScore?.toFixed(1) ?? '—'}
                      </div>
                    </div>
                  ) : entry?.status ? (
                    <span className="rounded-full border border-amber-500/30 px-3 py-1 text-xs font-medium text-amber-300">
                      Сдано
                    </span>
                  ) : (
                    <span className="rounded-full border border-slate-700 px-3 py-1 text-xs font-medium text-slate-500">
                      Не сдано
                    </span>
                  )}
                </div>

                {entry?.comment ? (
                  <div className="mt-4 rounded-xl border border-slate-800 bg-slate-900/70 p-4">
                    <div className="text-xs font-medium text-slate-400">Комментарий</div>
                    <p className="mt-1 whitespace-pre-wrap text-sm leading-6 text-slate-300">
                      {entry.comment}
                    </p>
                  </div>
                ) : null}

                {entry?.gradedAt ? (
                  <div className="mt-3 text-xs text-slate-500">
                    Оценено {formatDateTime(entry.gradedAt)}
                  </div>
                ) : null}
              </article>
            )
          })}
        </div>
      )}
    </div>
  )
}

function getAssignmentTypeLabel(type: string) {
  switch (type) {
    case 'TEXT':
      return 'Текстовое'
    case 'FILE':
      return 'Файловое'
    case 'CODE':
      return 'Кодовое'
    default:
      return type
  }
}
