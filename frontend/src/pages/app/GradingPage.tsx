import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useMemo, useState } from 'react'
import { Link, useParams } from 'react-router-dom'

import type { CodeAttemptResponse, SubmissionResponse } from '@/entities/course/types'
import { authApi } from '@/shared/api/auth'
import { coursesApi } from '@/shared/api/courses'
import { downloadPresignedFile } from '@/shared/lib/attachments'
import { getErrorMessage } from '@/shared/lib/errors'
import { formatDateTime, formatFileSize } from '@/shared/lib/format'
import { Button } from '@/shared/ui/Button'
import { EmptyState } from '@/shared/ui/EmptyState'
import { Input, Textarea } from '@/shared/ui/Input'
import { PageLoader } from '@/shared/ui/PageLoader'

type GradingTarget = {
  submissionId: string
  score: string
  comment: string
}

export function GradingPage() {
  const params = useParams<{ courseId: string; assignmentId: string }>()
  const courseId = params.courseId
  const assignmentId = params.assignmentId
  const queryClient = useQueryClient()
  const [gradingTarget, setGradingTarget] = useState<GradingTarget | null>(null)
  const [actionError, setActionError] = useState<string | null>(null)

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

  const assignmentQuery = useQuery({
    queryKey: ['assignment', assignmentId],
    queryFn: () => coursesApi.getAssignment(assignmentId!),
    enabled: Boolean(assignmentId),
  })

  const isCodeAssignment = assignmentQuery.data?.assignmentType === 'CODE'

  const submissionsQuery = useQuery({
    queryKey: ['all-submissions', assignmentId],
    queryFn: () => coursesApi.listAllSubmissions(assignmentId!),
    enabled: Boolean(assignmentId) && !isCodeAssignment,
  })

  const codeAttemptsQuery = useQuery({
    queryKey: ['all-code-attempts', assignmentId],
    queryFn: () => coursesApi.listAllCodeAttempts(assignmentId!),
    enabled: Boolean(assignmentId) && isCodeAssignment,
    refetchInterval: 5000,
  })

  const userIds = useMemo(() => {
    const ids = new Set<string>()
    for (const submission of submissionsQuery.data ?? []) {
      for (const memberId of submission.memberUserIds) {
        ids.add(memberId)
      }
    }
    for (const attempt of codeAttemptsQuery.data ?? []) {
      ids.add(attempt.studentUserId)
    }
    return Array.from(ids)
  }, [submissionsQuery.data, codeAttemptsQuery.data])

  const usersLookupQuery = useQuery({
    queryKey: ['users-lookup', userIds],
    queryFn: () => authApi.lookupByIds({ userIds }),
    enabled: userIds.length > 0,
  })

  const emailMap = useMemo(() => {
    const map = new Map<string, string>()
    for (const user of usersLookupQuery.data?.users ?? []) {
      map.set(user.userId, user.email)
    }
    return map
  }, [usersLookupQuery.data])

  const gradeMutation = useMutation({
    mutationFn: (target: GradingTarget) =>
      coursesApi.gradeSubmission(target.submissionId, {
        score: Number(target.score),
        comment: target.comment.trim() || undefined,
      }),
    onSuccess: () => {
      setGradingTarget(null)
      queryClient.invalidateQueries({ queryKey: ['all-submissions', assignmentId] })
    },
  })

  const retryAttemptMutation = useMutation({
    mutationFn: (attemptId: string) => coursesApi.retryCodeAttempt(attemptId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['all-code-attempts', assignmentId] })
    },
    onError: (error) => setActionError(getErrorMessage(error, 'Не удалось перезапустить попытку')),
  })

  const downloadLogMutation = useMutation({
    mutationFn: async (attemptId: string) => {
      const payload = await coursesApi.getCodeAttemptLog(attemptId)
      await downloadPresignedFile(payload)
    },
    onError: (error) => setActionError(getErrorMessage(error, 'Не удалось скачать лог')),
  })

  const downloadAttachmentMutation = useMutation({
    mutationFn: async (attachmentId: string) => {
      const payload = await coursesApi.presignDownloadAttachment(attachmentId)
      await downloadPresignedFile(payload)
    },
    onError: (error) => setActionError(getErrorMessage(error, 'Не удалось скачать файл')),
  })

  if (!courseId || !assignmentId) {
    return <EmptyState title="Не найдено" description="Отсутствуют параметры маршрута." />
  }

  if (
    courseQuery.isPending ||
    membershipQuery.isPending ||
    assignmentQuery.isPending
  ) {
    return <PageLoader label="Загружаем задание..." />
  }

  const firstError = [
    courseQuery.error,
    membershipQuery.error,
    assignmentQuery.error,
  ].find(Boolean)

  if (firstError) {
    return <EmptyState title="Ошибка" description={getErrorMessage(firstError)} />
  }

  const assignment = assignmentQuery.data!
  const membership = membershipQuery.data!

  if (membership.roleInCourse === 'STUDENT') {
    return (
      <EmptyState
        title="Доступ ограничен"
        description="Страница проверки доступна только преподавателям и ассистентам."
      />
    )
  }

  const breadcrumb = (
    <div className="text-sm text-slate-400">
      <Link
        to={`/app/courses/${courseId}`}
        className="text-blue-400 transition hover:text-blue-300"
      >
        {courseQuery.data!.title}
      </Link>
      {' / '}
      <Link
        to={`/app/courses/${courseId}/assignments/${assignmentId}`}
        className="text-blue-400 transition hover:text-blue-300"
      >
        {assignment.title}
      </Link>
    </div>
  )

  const errorBanner = actionError ? (
    <div className="flex items-start justify-between rounded-2xl border border-rose-500/30 bg-rose-500/10 px-4 py-3 text-sm text-rose-200">
      <span>{actionError}</span>
      <button
        className="ml-4 shrink-0 text-rose-400 transition hover:text-rose-200"
        onClick={() => setActionError(null)}
      >
        ✕
      </button>
    </div>
  ) : null

  if (isCodeAssignment) {
    return (
      <CodeGradingView
        courseId={courseId}
        assignmentId={assignmentId}
        assignment={assignment}
        breadcrumb={breadcrumb}
        codeAttemptsQuery={codeAttemptsQuery}
        emailMap={emailMap}
        retryAttemptMutation={retryAttemptMutation}
        downloadLogMutation={downloadLogMutation}
        errorBanner={errorBanner}
      />
    )
  }

  if (submissionsQuery.isPending) {
    return <PageLoader label="Загружаем сдачи..." />
  }

  if (submissionsQuery.isError) {
    return <EmptyState title="Ошибка" description={getErrorMessage(submissionsQuery.error)} />
  }

  const submissions = submissionsQuery.data!

  return (
    <div className="space-y-8">
      {errorBanner}
      <section className="flex flex-wrap items-start justify-between gap-4">
        <div>
          {breadcrumb}
          <h1 className="mt-2 text-2xl font-bold text-white">
            Проверка: {assignment.title}
          </h1>
          <p className="mt-1 text-sm text-slate-400">
            {submissions.length} сдач{submissions.length === 1 ? 'а' : submissions.length < 5 ? 'и' : ''}
          </p>
        </div>
      </section>

      {submissions.length === 0 ? (
        <EmptyState title="Сдач пока нет" description="Ни один студент еще не отправил решение." />
      ) : (
        <div className="space-y-5">
          {submissions
            .slice()
            .sort((a, b) => new Date(b.updatedAt).getTime() - new Date(a.updatedAt).getTime())
            .map((submission) => (
              <SubmissionCard
                key={submission.submissionId}
                submission={submission}
                emailMap={emailMap}
                isGrading={gradingTarget?.submissionId === submission.submissionId}
                gradingTarget={gradingTarget}
                setGradingTarget={setGradingTarget}
                gradeMutation={gradeMutation}
                downloadAttachmentMutation={downloadAttachmentMutation}
              />
            ))}
        </div>
      )}
    </div>
  )
}

function CodeGradingView({
  courseId,
  assignmentId,
  assignment,
  breadcrumb,
  codeAttemptsQuery,
  emailMap,
  retryAttemptMutation,
  downloadLogMutation,
  errorBanner,
}: {
  courseId: string
  assignmentId: string
  assignment: { title: string }
  breadcrumb: React.ReactNode
  codeAttemptsQuery: ReturnType<typeof useQuery<CodeAttemptResponse[]>>
  emailMap: Map<string, string>
  retryAttemptMutation: ReturnType<typeof useMutation<CodeAttemptResponse, Error, string>>
  downloadLogMutation: ReturnType<typeof useMutation<void, Error, string>>
  errorBanner: React.ReactNode
}) {
  if (codeAttemptsQuery.isPending) {
    return <PageLoader label="Загружаем попытки..." />
  }

  if (codeAttemptsQuery.isError) {
    return <EmptyState title="Ошибка" description={getErrorMessage(codeAttemptsQuery.error)} />
  }

  const allAttempts = codeAttemptsQuery.data!

  const groupedByStudent = useMemo(() => {
    const map = new Map<string, CodeAttemptResponse[]>()
    for (const attempt of allAttempts) {
      const list = map.get(attempt.studentUserId) ?? []
      list.push(attempt)
      map.set(attempt.studentUserId, list)
    }
    return Array.from(map.entries()).map(([userId, attempts]) => ({
      userId,
      attempts: attempts.sort((a, b) => b.attemptNumber - a.attemptNumber),
      bestScore: Math.max(...attempts.map((a) => a.score ?? -1)),
      latestStatus: attempts.sort((a, b) => b.attemptNumber - a.attemptNumber)[0]?.status,
    }))
  }, [allAttempts])

  return (
    <div className="space-y-8">
      {errorBanner}
      <section className="flex flex-wrap items-start justify-between gap-4">
        <div>
          {breadcrumb}
          <h1 className="mt-2 text-2xl font-bold text-white">
            Проверка: {assignment.title}
          </h1>
          <p className="mt-1 text-sm text-slate-400">
            {allAttempts.length} попыток от {groupedByStudent.length} студентов
          </p>
        </div>
      </section>

      {groupedByStudent.length === 0 ? (
        <EmptyState title="Попыток пока нет" description="Ни один студент еще не отправил решение на проверку." />
      ) : (
        <div className="space-y-6">
          {groupedByStudent.map(({ userId, attempts, bestScore }) => (
            <article
              key={userId}
              className="rounded-3xl border border-slate-800 bg-slate-950/70 p-6"
            >
              <div className="flex flex-wrap items-start justify-between gap-4">
                <div>
                  <div className="text-sm font-semibold text-white">
                    {emailMap.get(userId) ?? userId.slice(0, 8)}
                  </div>
                  <div className="mt-1 text-sm text-slate-400">
                    {attempts.length} попыток
                    {bestScore >= 0 ? ` · лучший результат: ${bestScore}` : ''}
                  </div>
                </div>
                <Link
                  to={`/app/courses/${courseId}/assignments/${assignmentId}`}
                  className="text-sm text-blue-400 transition hover:text-blue-300"
                >
                  К заданию
                </Link>
              </div>

              <div className="mt-5 space-y-3">
                {attempts.map((attempt) => (
                  <CodeAttemptRow
                    key={attempt.attemptId}
                    attempt={attempt}
                    retryAttemptMutation={retryAttemptMutation}
                    downloadLogMutation={downloadLogMutation}
                  />
                ))}
              </div>
            </article>
          ))}
        </div>
      )}
    </div>
  )
}

function SubmissionCard({
  submission,
  emailMap,
  isGrading,
  gradingTarget,
  setGradingTarget,
  gradeMutation,
  downloadAttachmentMutation,
}: {
  submission: SubmissionResponse
  emailMap: Map<string, string>
  isGrading: boolean
  gradingTarget: GradingTarget | null
  setGradingTarget: (target: GradingTarget | null) => void
  gradeMutation: ReturnType<typeof useMutation<SubmissionResponse, Error, GradingTarget>>
  downloadAttachmentMutation: ReturnType<typeof useMutation<void, Error, string>>
}) {
  const memberEmails = submission.memberUserIds.map((id) => emailMap.get(id) ?? id).join(', ')

  return (
    <article className="rounded-3xl border border-slate-800 bg-slate-950/70 p-6">
      <div className="flex flex-wrap items-start justify-between gap-4">
        <div>
          <div className="text-sm font-semibold text-white">{memberEmails}</div>
          <div className="mt-1 text-sm text-slate-400">
            {submission.status === 'GRADED' ? 'Проверено' : 'Ожидает проверки'}
            {' · '}
            {formatDateTime(submission.updatedAt)}
            {submission.score != null ? ` · Оценка: ${submission.score}` : ''}
          </div>
        </div>
        <span
          className={`rounded-full border px-3 py-1 text-xs font-medium ${
            submission.status === 'GRADED'
              ? 'border-green-500/30 text-green-300'
              : 'border-amber-500/30 text-amber-300'
          }`}
        >
          {submission.status === 'GRADED' ? 'Проверено' : 'На проверке'}
        </span>
      </div>

      {submission.textAnswer ? (
        <div className="mt-5">
          <div className="text-sm font-medium text-slate-300">Ответ</div>
          <p className="mt-2 whitespace-pre-wrap rounded-2xl border border-slate-800 bg-slate-900/70 p-4 text-sm leading-6 text-slate-200">
            {submission.textAnswer}
          </p>
        </div>
      ) : null}

      {submission.attachments.length > 0 ? (
        <div className="mt-5 space-y-2">
          <div className="text-sm font-medium text-slate-300">Файлы</div>
          {submission.attachments.map((attachment) => (
            <div
              key={attachment.attachmentId}
              className="flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-slate-800 bg-slate-900/70 p-4"
            >
              <div>
                <div className="text-sm font-medium text-white">{attachment.fileName}</div>
                <div className="mt-0.5 text-xs text-slate-400">{formatFileSize(attachment.fileSize)}</div>
              </div>
              <Button
                variant="secondary"
                onClick={() => downloadAttachmentMutation.mutate(attachment.attachmentId)}
                isLoading={downloadAttachmentMutation.isPending}
              >
                Скачать
              </Button>
            </div>
          ))}
        </div>
      ) : null}

      {submission.comment ? (
        <div className="mt-5">
          <div className="text-sm font-medium text-slate-300">Комментарий проверяющего</div>
          <p className="mt-2 whitespace-pre-wrap text-sm leading-6 text-slate-400">
            {submission.comment}
          </p>
        </div>
      ) : null}

      <div className="mt-5 border-t border-slate-800 pt-5">
        {isGrading && gradingTarget ? (
          <div className="space-y-4">
            <div className="grid gap-4 md:grid-cols-[180px_1fr]">
              <label className="block space-y-2">
                <span className="text-sm text-slate-300">Оценка (0–100)</span>
                <Input
                  type="number"
                  min="0"
                  max="100"
                  value={gradingTarget.score}
                  onChange={(event) =>
                    setGradingTarget({ ...gradingTarget, score: event.target.value })
                  }
                />
              </label>
              <label className="block space-y-2">
                <span className="text-sm text-slate-300">Комментарий</span>
                <Textarea
                  rows={3}
                  value={gradingTarget.comment}
                  onChange={(event) =>
                    setGradingTarget({ ...gradingTarget, comment: event.target.value })
                  }
                />
              </label>
            </div>
            {gradeMutation.isError ? (
              <div className="rounded-2xl border border-rose-500/30 bg-rose-500/10 px-4 py-3 text-sm text-rose-200">
                {getErrorMessage(gradeMutation.error, 'Не удалось выставить оценку')}
              </div>
            ) : null}
            <div className="flex flex-wrap gap-3">
              <Button
                onClick={() => gradeMutation.mutate(gradingTarget)}
                isLoading={gradeMutation.isPending}
              >
                Сохранить оценку
              </Button>
              <Button variant="secondary" onClick={() => setGradingTarget(null)}>
                Отмена
              </Button>
            </div>
          </div>
        ) : (
          <Button
            variant="secondary"
            onClick={() =>
              setGradingTarget({
                submissionId: submission.submissionId,
                score: submission.score != null ? String(submission.score) : '',
                comment: submission.comment ?? '',
              })
            }
          >
            {submission.status === 'GRADED' ? 'Изменить оценку' : 'Оценить'}
          </Button>
        )}
      </div>
    </article>
  )
}

function CodeAttemptRow({
  attempt,
  retryAttemptMutation,
  downloadLogMutation,
}: {
  attempt: CodeAttemptResponse
  retryAttemptMutation: ReturnType<typeof useMutation<CodeAttemptResponse, Error, string>>
  downloadLogMutation: ReturnType<typeof useMutation<void, Error, string>>
}) {
  const statusLabels: Record<CodeAttemptResponse['status'], string> = {
    QUEUED: 'В очереди',
    RUNNING: 'Идет проверка',
    COMPLETED: 'Завершено',
    ERROR: 'Ошибка',
  }

  return (
    <div className="rounded-2xl border border-slate-800 bg-slate-900/70 p-4">
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <div className="text-sm font-medium text-white">
            Попытка #{attempt.attemptNumber}
          </div>
          <div className="mt-1 text-xs text-slate-400">
            PR #{attempt.pullRequestNumber} · {attempt.repositoryFullName} ·{' '}
            {formatDateTime(attempt.queuedAt)}
          </div>
        </div>
        <span
          className={`rounded-full border px-3 py-1 text-xs font-medium ${
            attempt.status === 'COMPLETED'
              ? 'border-green-500/30 text-green-300'
              : attempt.status === 'ERROR'
                ? 'border-rose-500/30 text-rose-300'
                : 'border-amber-500/30 text-amber-300'
          }`}
        >
          {statusLabels[attempt.status]}
        </span>
      </div>

      {attempt.testsTotal != null ? (
        <div className="mt-2 text-sm text-slate-300">
          Тесты: {attempt.testsPassed ?? 0}/{attempt.testsTotal}
          {attempt.score != null ? ` · Баллы: ${attempt.score}` : ''}
        </div>
      ) : null}

      {attempt.resultSummary ? (
        <p className="mt-2 text-sm text-slate-400">{attempt.resultSummary}</p>
      ) : null}

      {attempt.comment ? (
        <p className="mt-2 text-sm text-slate-400">{attempt.comment}</p>
      ) : null}

      <div className="mt-3 flex flex-wrap gap-2">
        <a
          className="inline-flex rounded-xl border border-slate-700 bg-slate-950 px-3 py-1.5 text-xs font-medium text-slate-100 transition hover:border-slate-600 hover:bg-slate-800"
          href={attempt.pullRequestUrl}
          target="_blank"
          rel="noreferrer"
        >
          Открыть PR
        </a>
        <Button
          variant="ghost"
          onClick={() => downloadLogMutation.mutate(attempt.attemptId)}
          isLoading={downloadLogMutation.isPending}
        >
          Скачать лог
        </Button>
        {(attempt.status === 'ERROR' || attempt.status === 'COMPLETED') ? (
          <Button
            variant="ghost"
            onClick={() => retryAttemptMutation.mutate(attempt.attemptId)}
            isLoading={retryAttemptMutation.isPending}
          >
            Перезапустить
          </Button>
        ) : null}
      </div>
    </div>
  )
}
