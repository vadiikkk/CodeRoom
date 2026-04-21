import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useEffect, useState, type ChangeEvent } from 'react'
import { useForm } from 'react-hook-form'
import { Link, useParams } from 'react-router-dom'
import { z } from 'zod'

import type {
  AssignmentResponse,
  AttachmentResponse,
  CodeAttemptResponse,
  SubmissionResponse,
} from '@/entities/course/types'
import { coursesApi } from '@/shared/api/courses'
import { downloadPresignedFile, uploadFileToPresignedUrl } from '@/shared/lib/attachments'
import { getErrorMessage } from '@/shared/lib/errors'
import { formatDateTime, formatFileSize } from '@/shared/lib/format'
import { Button } from '@/shared/ui/Button'
import { EmptyState } from '@/shared/ui/EmptyState'
import { FieldError } from '@/shared/ui/FieldError'
import { Input, Textarea } from '@/shared/ui/Input'
import { PageLoader } from '@/shared/ui/PageLoader'

const textSubmissionSchema = z.object({
  textAnswer: z
    .string()
    .trim()
    .min(1, 'Введите ответ')
    .max(100000, 'Ответ слишком длинный'),
})

const codeAttemptSchema = z.object({
  pullRequestUrl: z.url('Введите корректную ссылку на pull request').max(500),
})

export function AssignmentDetailsPage() {
  const queryClient = useQueryClient()
  const params = useParams<{ courseId: string; assignmentId: string }>()
  const courseId = params.courseId
  const assignmentId = params.assignmentId
  const [selectedFiles, setSelectedFiles] = useState<File[]>([])
  const [fileComment, setFileComment] = useState('')
  const [fileFormError, setFileFormError] = useState<string | null>(null)
  const [downloadError, setDownloadError] = useState<string | null>(null)

  const assignmentQuery = useQuery({
    queryKey: ['assignment', assignmentId],
    queryFn: () => coursesApi.getAssignment(assignmentId!),
    enabled: Boolean(assignmentId),
  })

  const membershipQuery = useQuery({
    queryKey: ['course-membership', courseId],
    queryFn: () => coursesApi.getMembership(courseId!),
    enabled: Boolean(courseId),
  })

  const textForm = useForm({
    resolver: zodResolver(textSubmissionSchema),
    defaultValues: {
      textAnswer: '',
    },
  })

  const codeAttemptForm = useForm({
    resolver: zodResolver(codeAttemptSchema),
    defaultValues: {
      pullRequestUrl: '',
    },
  })

  const isStudent = membershipQuery.data?.roleInCourse === 'STUDENT'
  const shouldLoadSubmission =
    Boolean(assignmentId) &&
    Boolean(isStudent) &&
    assignmentQuery.data?.assignmentType !== 'CODE'
  const shouldLoadCodeAttempts =
    Boolean(assignmentId) &&
    Boolean(isStudent) &&
    assignmentQuery.data?.assignmentType === 'CODE'

  const mySubmissionQuery = useQuery({
    queryKey: ['submission', 'me', assignmentId],
    queryFn: () => coursesApi.getMySubmission(assignmentId!),
    enabled: shouldLoadSubmission,
  })

  const myCodeAttemptsQuery = useQuery({
    queryKey: ['code-attempts', 'me', assignmentId],
    queryFn: () => coursesApi.getMyCodeAttempts(assignmentId!),
    enabled: shouldLoadCodeAttempts,
    refetchInterval: 4000,
  })

  useEffect(() => {
    if (mySubmissionQuery.data?.textAnswer && assignmentQuery.data?.assignmentType === 'TEXT') {
      textForm.reset({
        textAnswer: mySubmissionQuery.data.textAnswer,
      })
    }
  }, [assignmentQuery.data?.assignmentType, mySubmissionQuery.data?.textAnswer, textForm])

  const downloadAttachmentMutation = useMutation({
    mutationFn: async (attachmentId: string) => {
      const payload = await coursesApi.presignDownloadAttachment(attachmentId)
      await downloadPresignedFile(payload)
    },
    onError: (error) => setDownloadError(getErrorMessage(error, 'Не удалось скачать файл')),
  })

  const submitTextMutation = useMutation({
    mutationFn: async (values: z.infer<typeof textSubmissionSchema>) => {
      return coursesApi.createOrUpdateSubmission(assignmentId!, {
        textAnswer: values.textAnswer.trim(),
        attachmentIds: [],
      })
    },
    onSuccess: (submission) => {
      textForm.reset({
        textAnswer: submission.textAnswer ?? '',
      })
      queryClient.setQueryData(['submission', 'me', assignmentId], submission)
    },
  })

  const submitFileMutation = useMutation({
    mutationFn: async () => {
      if (selectedFiles.length === 0) {
        throw new Error('Выберите хотя бы один файл')
      }

      const uploadedAttachmentIds: string[] = []

      for (const file of selectedFiles) {
        const presign = await coursesApi.presignUploadAttachment({
          courseId: courseId!,
          fileName: file.name,
          contentType: file.type || undefined,
          fileSize: file.size,
        })

        await uploadFileToPresignedUrl(presign, file)
        uploadedAttachmentIds.push(presign.attachmentId)
      }

      return coursesApi.createOrUpdateSubmission(assignmentId!, {
        textAnswer: fileComment.trim() || undefined,
        attachmentIds: uploadedAttachmentIds,
      })
    },
    onSuccess: (submission) => {
      setSelectedFiles([])
      setFileComment(submission.textAnswer ?? '')
      setFileFormError(null)
      queryClient.setQueryData(['submission', 'me', assignmentId], submission)
    },
    onError: (error) => {
      setFileFormError(getErrorMessage(error, 'Не удалось отправить файлы'))
    },
  })

  const createCodeAttemptMutation = useMutation({
    mutationFn: (values: z.infer<typeof codeAttemptSchema>) =>
      coursesApi.createCodeAttempt(assignmentId!, {
        pullRequestUrl: values.pullRequestUrl.trim(),
      }),
    onSuccess: () => {
      codeAttemptForm.reset()
      queryClient.invalidateQueries({ queryKey: ['code-attempts', 'me', assignmentId] })
    },
  })

  const downloadAttemptLogMutation = useMutation({
    mutationFn: async (attemptId: string) => {
      const payload = await coursesApi.getCodeAttemptLog(attemptId)
      await downloadPresignedFile(payload)
    },
    onError: (error) => setDownloadError(getErrorMessage(error, 'Не удалось скачать лог')),
  })

  if (!courseId || !assignmentId) {
    return (
      <EmptyState
        title="Задание не найдено"
        description="В адресе отсутствует идентификатор задания."
      />
    )
  }

  if (assignmentQuery.isPending || membershipQuery.isPending) {
    return <PageLoader label="Загружаем задание..." />
  }

  if (assignmentQuery.isError || membershipQuery.isError) {
    return (
      <EmptyState
        title="Не удалось загрузить задание"
        description={getErrorMessage(assignmentQuery.error ?? membershipQuery.error)}
      />
    )
  }

  const assignment = assignmentQuery.data
  const mySubmission = mySubmissionQuery.data ?? null
  const myCodeAttempts = [...(myCodeAttemptsQuery.data ?? [])].sort(
    (left, right) => right.attemptNumber - left.attemptNumber,
  )
  const submissionLocked = mySubmission?.status === 'GRADED'
  const canSubmit = membershipQuery.data?.roleInCourse === 'STUDENT'

  return (
    <div className="space-y-8">
      {downloadError ? (
        <div className="flex items-start justify-between rounded-2xl border border-rose-500/30 bg-rose-500/10 px-4 py-3 text-sm text-rose-200">
          <span>{downloadError}</span>
          <button
            className="ml-4 shrink-0 text-rose-400 transition hover:text-rose-200"
            onClick={() => setDownloadError(null)}
          >
            ✕
          </button>
        </div>
      ) : null}

      {mySubmissionQuery.isError ? (
        <div className="rounded-2xl border border-amber-500/30 bg-amber-500/10 px-4 py-3 text-sm text-amber-200">
          Не удалось загрузить текущую сдачу: {getErrorMessage(mySubmissionQuery.error)}
        </div>
      ) : null}

      {myCodeAttemptsQuery.isError ? (
        <div className="rounded-2xl border border-amber-500/30 bg-amber-500/10 px-4 py-3 text-sm text-amber-200">
          Не удалось загрузить попытки: {getErrorMessage(myCodeAttemptsQuery.error)}
        </div>
      ) : null}

      <section className="rounded-3xl border border-slate-800 bg-slate-950/70 p-8">
        <Link className="text-sm text-blue-300 hover:text-blue-200" to={`/app/courses/${courseId}`}>
          ← Назад к курсу
        </Link>
        <h1 className="mt-4 text-3xl font-semibold text-white">{assignment.title}</h1>
        <p className="mt-3 text-sm text-slate-400">
          {assignment.description || 'Описание задания не заполнено.'}
        </p>
        <div className="mt-6 flex flex-wrap gap-2 text-xs text-slate-300">
          <span className="rounded-full border border-slate-700 px-3 py-1">
            Тип: {getAssignmentTypeLabel(assignment.assignmentType)}
          </span>
          <span className="rounded-full border border-slate-700 px-3 py-1">
            Формат: {getWorkTypeLabel(assignment.workType)}
          </span>
          <span className="rounded-full border border-slate-700 px-3 py-1">
            Вес: {assignment.weight}
          </span>
          <span className="rounded-full border border-slate-700 px-3 py-1">
            {assignment.isVisible ? 'Опубликовано' : 'Скрыто'}
          </span>
        </div>
      </section>

      {!canSubmit ? (
        <Link
          to={`/app/courses/${courseId}/assignments/${assignmentId}/grading`}
          className="inline-flex rounded-xl border border-blue-500/40 bg-blue-500/10 px-4 py-2.5 text-sm font-medium text-blue-100 transition hover:bg-blue-500/20"
        >
          Проверить сдачи студентов
        </Link>
      ) : null}

      <section className="grid gap-6 xl:grid-cols-[1.1fr_0.9fr]">
        <div className="space-y-6">
          <article className="rounded-3xl border border-slate-800 bg-slate-950/70 p-6">
            <h2 className="text-xl font-semibold text-white">Параметры</h2>
            <div className="mt-4 grid gap-3 md:grid-cols-2">
              <InfoCard label="Тип задания" value={getAssignmentTypeLabel(assignment.assignmentType)} />
              <InfoCard label="Формат работы" value={getWorkTypeLabel(assignment.workType)} />
              <InfoCard label="Дедлайн" value={formatDateTime(assignment.deadlineAt)} />
              <InfoCard label="Обновлено" value={formatDateTime(assignment.updatedAt)} />
            </div>
          </article>

          {assignment.assignmentType === 'CODE' && assignment.code ? (
            <article className="rounded-3xl border border-slate-800 bg-slate-950/70 p-6">
              <h2 className="text-xl font-semibold text-white">Репозиторий задания</h2>
              <div className="mt-4 grid gap-3 md:grid-cols-2">
                <InfoCard label="Язык" value={assignment.code.language} />
                <InfoCard label="Максимум попыток" value={String(assignment.code.maxAttempts)} />
                <InfoCard label="Репозиторий" value={assignment.code.repositoryFullName} />
                <InfoCard label="Ветка по умолчанию" value={assignment.code.defaultBranch} />
              </div>
              <a
                className="mt-5 inline-flex text-sm text-blue-300 hover:text-blue-200"
                href={assignment.code.repositoryUrl}
                target="_blank"
                rel="noreferrer"
              >
                Открыть GitHub repository
              </a>
            </article>
          ) : null}

          {canSubmit ? (
            <StudentSubmissionSection
              assignment={assignment}
              mySubmission={mySubmission}
              myCodeAttempts={myCodeAttempts}
              submissionLocked={submissionLocked}
              textForm={textForm}
              fileComment={fileComment}
              selectedFiles={selectedFiles}
              fileFormError={fileFormError}
              setFileComment={setFileComment}
              setSelectedFiles={setSelectedFiles}
              setFileFormError={setFileFormError}
              submitTextMutation={submitTextMutation}
              submitFileMutation={submitFileMutation}
              codeAttemptForm={codeAttemptForm}
              createCodeAttemptMutation={createCodeAttemptMutation}
              downloadAttemptLogMutation={downloadAttemptLogMutation}
            />
          ) : (
            <article className="rounded-3xl border border-slate-800 bg-slate-950/70 p-6">
              <h2 className="text-xl font-semibold text-white">Сдача задания</h2>
              <p className="mt-4 text-sm leading-6 text-slate-300">
                Сдача задания доступна только студентам.
              </p>
            </article>
          )}
        </div>

        <aside className="space-y-6">
          <div className="rounded-3xl border border-slate-800 bg-slate-950/70 p-6">
            <h2 className="text-xl font-semibold text-white">Вложения</h2>
            {assignment.attachments.length === 0 ? (
              <p className="mt-4 text-sm text-slate-400">У задания нет приложенных файлов.</p>
            ) : (
              <div className="mt-4 space-y-3">
                {assignment.attachments.map((attachment) => (
                  <div
                    key={attachment.attachmentId}
                    className="rounded-2xl border border-slate-800 bg-slate-900/70 p-4"
                  >
                    <div className="font-medium text-white">{attachment.fileName}</div>
                    <div className="mt-2 text-sm text-slate-400">
                      {formatFileSize(attachment.fileSize)}
                    </div>
                    <Button
                      className="mt-4"
                      variant="secondary"
                      onClick={() => downloadAttachmentMutation.mutate(attachment.attachmentId)}
                      isLoading={downloadAttachmentMutation.isPending}
                    >
                      Скачать
                    </Button>
                  </div>
                ))}
              </div>
            )}
          </div>

          <div className="rounded-3xl border border-slate-800 bg-slate-950/70 p-6">
            <h2 className="text-xl font-semibold text-white">Как выполнить задание</h2>
            <p className="mt-4 text-sm text-slate-400">
              {getAssignmentHelpText(assignment)}
            </p>
          </div>
        </aside>
      </section>
    </div>
  )
}

interface StudentSubmissionSectionProps {
  assignment: AssignmentResponse
  mySubmission: SubmissionResponse | null
  myCodeAttempts: CodeAttemptResponse[]
  submissionLocked: boolean
  textForm: ReturnType<typeof useForm<z.infer<typeof textSubmissionSchema>>>
  fileComment: string
  selectedFiles: File[]
  fileFormError: string | null
  setFileComment: (value: string) => void
  setSelectedFiles: (files: File[]) => void
  setFileFormError: (value: string | null) => void
  submitTextMutation: ReturnType<typeof useMutation<SubmissionResponse, Error, z.infer<typeof textSubmissionSchema>>>
  submitFileMutation: ReturnType<typeof useMutation<SubmissionResponse, Error, void>>
  codeAttemptForm: ReturnType<typeof useForm<z.infer<typeof codeAttemptSchema>>>
  createCodeAttemptMutation: ReturnType<typeof useMutation<CodeAttemptResponse, Error, z.infer<typeof codeAttemptSchema>>>
  downloadAttemptLogMutation: ReturnType<typeof useMutation<void, Error, string>>
}

function StudentSubmissionSection({
  assignment,
  mySubmission,
  myCodeAttempts,
  submissionLocked,
  textForm,
  fileComment,
  selectedFiles,
  fileFormError,
  setFileComment,
  setSelectedFiles,
  setFileFormError,
  submitTextMutation,
  submitFileMutation,
  codeAttemptForm,
  createCodeAttemptMutation,
  downloadAttemptLogMutation,
}: StudentSubmissionSectionProps) {
  if (assignment.assignmentType === 'TEXT') {
    return (
      <article className="rounded-3xl border border-slate-800 bg-slate-950/70 p-6">
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div>
            <h2 className="text-xl font-semibold text-white">Ваш ответ</h2>
            <p className="mt-2 text-sm text-slate-400">
              Введите текст ответа и отправьте его на проверку.
            </p>
          </div>
          {mySubmission ? (
            <StatusChip label={getSubmissionStatusLabel(mySubmission.status)} />
          ) : null}
        </div>

        {mySubmission ? <SubmissionSummary submission={mySubmission} /> : null}

        {submissionLocked ? (
          <LockedSubmissionNotice />
        ) : (
          <form
            className="mt-6 space-y-4"
            onSubmit={textForm.handleSubmit((values) => submitTextMutation.mutate(values))}
          >
            <label className="block space-y-2">
              <span className="text-sm text-slate-300">Ответ</span>
              <Textarea rows={12} {...textForm.register('textAnswer')} />
              <FieldError message={textForm.formState.errors.textAnswer?.message} />
            </label>

            {submitTextMutation.isError ? (
              <div className="rounded-2xl border border-rose-500/30 bg-rose-500/10 px-4 py-3 text-sm text-rose-200">
                {getErrorMessage(submitTextMutation.error, 'Не удалось отправить ответ')}
              </div>
            ) : null}

            <Button type="submit" isLoading={submitTextMutation.isPending}>
              {mySubmission ? 'Обновить ответ' : 'Отправить ответ'}
            </Button>
          </form>
        )}
      </article>
    )
  }

  if (assignment.assignmentType === 'FILE') {
    return (
      <article className="rounded-3xl border border-slate-800 bg-slate-950/70 p-6">
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div>
            <h2 className="text-xl font-semibold text-white">Сдача файлами</h2>
            <p className="mt-2 text-sm text-slate-400">
              Прикрепите один или несколько файлов и отправьте их на проверку.
            </p>
          </div>
          {mySubmission ? (
            <StatusChip label={getSubmissionStatusLabel(mySubmission.status)} />
          ) : null}
        </div>

        {mySubmission ? <SubmissionSummary submission={mySubmission} /> : null}

        {submissionLocked ? (
          <LockedSubmissionNotice />
        ) : (
          <div className="mt-6 space-y-4">
            <label className="block space-y-2">
              <span className="text-sm text-slate-300">Комментарий к сдаче</span>
              <Textarea
                rows={5}
                value={fileComment}
                onChange={(event) => setFileComment(event.target.value)}
                placeholder="При необходимости добавьте пояснение к файлам."
              />
            </label>

            <label className="block space-y-2">
              <span className="text-sm text-slate-300">Файлы</span>
              <Input
                type="file"
                multiple
                onChange={(event: ChangeEvent<HTMLInputElement>) => {
                  const files = Array.from(event.target.files ?? [])

                  setSelectedFiles(files)
                  setFileFormError(null)
                }}
              />
            </label>

            {selectedFiles.length > 0 ? (
              <div className="rounded-2xl border border-slate-800 bg-slate-900/70 p-4">
                <div className="text-sm font-medium text-white">Будут отправлены</div>
                <ul className="mt-3 space-y-2 text-sm text-slate-300">
                  {selectedFiles.map((file) => (
                    <li key={`${file.name}-${file.size}`}>
                      {file.name} · {formatFileSize(file.size)}
                    </li>
                  ))}
                </ul>
              </div>
            ) : null}

            {fileFormError ? (
              <div className="rounded-2xl border border-rose-500/30 bg-rose-500/10 px-4 py-3 text-sm text-rose-200">
                {fileFormError}
              </div>
            ) : null}

            <Button
              type="button"
              isLoading={submitFileMutation.isPending}
              onClick={() => {
                if (selectedFiles.length === 0) {
                  setFileFormError('Выберите хотя бы один файл')
                  return
                }

                submitFileMutation.mutate()
              }}
            >
              {mySubmission ? 'Обновить файлы' : 'Отправить файлы'}
            </Button>
          </div>
        )}
      </article>
    )
  }

  const latestAttempt = myCodeAttempts[0]

  return (
    <article className="rounded-3xl border border-slate-800 bg-slate-950/70 p-6">
      <h2 className="text-xl font-semibold text-white">Отправка решения</h2>
      <p className="mt-2 text-sm text-slate-400">
        Укажите ссылку на pull request в репозитории задания. После отправки
        проверка запустится автоматически.
      </p>

      <form
        className="mt-6 space-y-4"
        onSubmit={codeAttemptForm.handleSubmit((values) =>
          createCodeAttemptMutation.mutate(values),
        )}
      >
        <label className="block space-y-2">
          <span className="text-sm text-slate-300">Ссылка на pull request</span>
          <Input
            type="text"
            placeholder="https://github.com/org/repo/pull/15"
            {...codeAttemptForm.register('pullRequestUrl')}
          />
          <FieldError message={codeAttemptForm.formState.errors.pullRequestUrl?.message} />
        </label>

        {createCodeAttemptMutation.isError ? (
          <div className="rounded-2xl border border-rose-500/30 bg-rose-500/10 px-4 py-3 text-sm text-rose-200">
            {getErrorMessage(createCodeAttemptMutation.error, 'Не удалось создать попытку')}
          </div>
        ) : null}

        <Button type="submit" isLoading={createCodeAttemptMutation.isPending}>
          Отправить на проверку
        </Button>
      </form>

      {latestAttempt ? (
        <div className="mt-8 space-y-4">
          <h3 className="text-lg font-semibold text-white">Попытки</h3>
          {myCodeAttempts.map((attempt) => (
            <div
              key={attempt.attemptId}
              className="rounded-2xl border border-slate-800 bg-slate-900/70 p-5"
            >
              <div className="flex flex-wrap items-start justify-between gap-4">
                <div>
                  <div className="text-sm font-semibold text-white">
                    Попытка #{attempt.attemptNumber}
                  </div>
                  <div className="mt-1 text-sm text-slate-400">
                    PR #{attempt.pullRequestNumber} · {formatDateTime(attempt.queuedAt)}
                  </div>
                </div>
                <StatusChip label={getCodeAttemptStatusLabel(attempt.status)} />
              </div>

              <div className="mt-4 grid gap-3 md:grid-cols-2">
                <InfoCard label="Репозиторий" value={attempt.repositoryFullName} />
                <InfoCard label="Ссылка на PR" value={attempt.pullRequestUrl} />
                <InfoCard
                  label="Тесты"
                  value={
                    attempt.testsTotal != null
                      ? `${attempt.testsPassed ?? 0}/${attempt.testsTotal}`
                      : 'Пока нет данных'
                  }
                />
                <InfoCard
                  label="Оценка"
                  value={attempt.score != null ? String(attempt.score) : 'Пока нет данных'}
                />
              </div>

            
              {attempt.comment ? (
                <p className="mt-3 text-sm leading-6 text-slate-300">{attempt.comment}</p>
              ) : null}

              <div className="mt-4 flex flex-wrap gap-3">
                <a
                  className="inline-flex rounded-xl border border-slate-700 bg-slate-950 px-4 py-2 text-sm font-medium text-slate-100 transition hover:border-slate-600 hover:bg-slate-800"
                  href={attempt.pullRequestUrl}
                  target="_blank"
                  rel="noreferrer"
                >
                  Открыть pull request
                </a>
                <Button
                  variant="secondary"
                  onClick={() => downloadAttemptLogMutation.mutate(attempt.attemptId)}
                  isLoading={downloadAttemptLogMutation.isPending}
                >
                  Скачать лог
                </Button>
              </div>
            </div>
          ))}
        </div>
      ) : null}
    </article>
  )
}

interface InfoCardProps {
  label: string
  value: string
}

function InfoCard({ label, value }: InfoCardProps) {
  return (
    <div className="rounded-2xl border border-slate-800 bg-slate-900/70 p-4">
      <div className="text-xs uppercase tracking-wide text-slate-500">{label}</div>
      <div className="mt-2 text-sm font-medium text-white">{value}</div>
    </div>
  )
}

function SubmissionSummary({ submission }: { submission: SubmissionResponse }) {
  return (
    <div className="mt-6 rounded-2xl border border-slate-800 bg-slate-900/70 p-5">
      <div className="grid gap-3 md:grid-cols-2">
        <InfoCard label="Статус" value={getSubmissionStatusLabel(submission.status)} />
        <InfoCard label="Обновлено" value={formatDateTime(submission.updatedAt)} />
        <InfoCard
          label="Оценка"
          value={submission.score != null ? String(submission.score) : 'Пока нет оценки'}
        />
        <InfoCard
          label="Проверено"
          value={submission.gradedAt ? formatDateTime(submission.gradedAt) : 'Пока нет'}
        />
      </div>

      {submission.textAnswer ? (
        <div className="mt-5">
          <div className="text-sm font-medium text-white">Текущий ответ</div>
          <p className="mt-2 whitespace-pre-wrap text-sm leading-6 text-slate-300">
            {submission.textAnswer}
          </p>
        </div>
      ) : null}

      {submission.attachments.length > 0 ? (
        <div className="mt-5">
          <div className="text-sm font-medium text-white">Прикрепленные файлы</div>
          <div className="mt-3 space-y-3">
            {submission.attachments.map((attachment) => (
              <SubmissionAttachmentRow key={attachment.attachmentId} attachment={attachment} />
            ))}
          </div>
        </div>
      ) : null}

      {submission.comment ? (
        <div className="mt-5 rounded-2xl border border-slate-800 bg-slate-950/70 p-4">
          <div className="text-sm font-medium text-white">Комментарий проверяющего</div>
          <p className="mt-2 whitespace-pre-wrap text-sm leading-6 text-slate-300">
            {submission.comment}
          </p>
        </div>
      ) : null}
    </div>
  )
}

function SubmissionAttachmentRow({ attachment }: { attachment: AttachmentResponse }) {
  const downloadAttachmentMutation = useMutation({
    mutationFn: async () => {
      const payload = await coursesApi.presignDownloadAttachment(attachment.attachmentId)
      await downloadPresignedFile(payload)
    },
  })

  return (
    <div className="flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-slate-800 bg-slate-950/60 p-4">
      <div>
        <div className="text-sm font-medium text-white">{attachment.fileName}</div>
        <div className="mt-1 text-sm text-slate-400">{formatFileSize(attachment.fileSize)}</div>
      </div>
      <Button
        variant="secondary"
        onClick={() => downloadAttachmentMutation.mutate()}
        isLoading={downloadAttachmentMutation.isPending}
      >
        Скачать
      </Button>
    </div>
  )
}

function LockedSubmissionNotice() {
  return (
    <div className="mt-6 rounded-2xl border border-amber-500/30 bg-amber-500/10 px-4 py-3 text-sm text-amber-100">
      Эта работа уже проверена.
    </div>
  )
}

function StatusChip({ label }: { label: string }) {
  return (
    <span className="rounded-full border border-slate-700 px-3 py-1 text-xs font-medium text-slate-200">
      {label}
    </span>
  )
}

function getAssignmentTypeLabel(type: AssignmentResponse['assignmentType']) {
  switch (type) {
    case 'TEXT':
      return 'Текстовое'
    case 'FILE':
      return 'Файловое'
    case 'CODE':
      return 'Кодовое'
  }
}

function getWorkTypeLabel(type: AssignmentResponse['workType']) {
  switch (type) {
    case 'INDIVIDUAL':
      return 'Индивидуальная работа'
    case 'GROUP':
      return 'Групповая работа'
  }
}

function getSubmissionStatusLabel(status: SubmissionResponse['status']) {
  switch (status) {
    case 'SUBMITTED':
      return 'Отправлено'
    case 'GRADED':
      return 'Проверено'
  }
}

function getCodeAttemptStatusLabel(status: CodeAttemptResponse['status']) {
  switch (status) {
    case 'QUEUED':
      return 'В очереди'
    case 'RUNNING':
      return 'Идет проверка'
    case 'COMPLETED':
      return 'Проверка завершена'
    case 'ERROR':
      return 'Ошибка проверки'
  }
}

function getAssignmentHelpText(assignment: AssignmentResponse) {
  switch (assignment.assignmentType) {
    case 'TEXT':
      return 'Подготовьте текстовый ответ и отправьте его через форму на этой странице.'
    case 'FILE':
      return 'Подготовьте нужные файлы и прикрепите их к сдаче. При необходимости добавьте короткий комментарий.'
    case 'CODE':
      return 'Создайте pull request в репозитории задания и отправьте ссылку на него для автоматической проверки.'
  }
}
