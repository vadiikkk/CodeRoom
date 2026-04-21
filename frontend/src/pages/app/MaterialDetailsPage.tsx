import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useEffect, useState, type ChangeEvent } from 'react'
import { Link, useParams } from 'react-router-dom'

import type { AttachmentResponse } from '@/entities/course/types'
import { coursesApi } from '@/shared/api/courses'
import { downloadPresignedFile, uploadFileToPresignedUrl } from '@/shared/lib/attachments'
import { getErrorMessage } from '@/shared/lib/errors'
import { formatDateTime, formatFileSize } from '@/shared/lib/format'
import { Button } from '@/shared/ui/Button'
import { EmptyState } from '@/shared/ui/EmptyState'
import { Input, Textarea } from '@/shared/ui/Input'
import { PageLoader } from '@/shared/ui/PageLoader'

export function MaterialDetailsPage() {
  const queryClient = useQueryClient()
  const params = useParams<{ courseId: string; materialId: string }>()
  const courseId = params.courseId
  const materialId = params.materialId

  const [isEditing, setIsEditing] = useState(false)
  const [title, setTitle] = useState('')
  const [description, setDescription] = useState('')
  const [body, setBody] = useState('')
  const [isVisible, setIsVisible] = useState(true)
  const [existingAttachments, setExistingAttachments] = useState<AttachmentResponse[]>([])
  const [newFiles, setNewFiles] = useState<File[]>([])

  const materialQuery = useQuery({
    queryKey: ['material', materialId],
    queryFn: () => coursesApi.getMaterial(materialId!),
    enabled: Boolean(materialId),
  })

  const membershipQuery = useQuery({
    queryKey: ['course-membership', courseId],
    queryFn: () => coursesApi.getMembership(courseId!),
    enabled: Boolean(courseId),
  })

  const canEditMaterial = membershipQuery.data?.roleInCourse === 'TEACHER'

  useEffect(() => {
    if (materialQuery.data) {
      const m = materialQuery.data
      setTitle(m.title)
      setDescription(m.description ?? '')
      setBody(m.body ?? '')
      setIsVisible(m.isVisible)
      setExistingAttachments(m.attachments)
    }
  }, [materialQuery.data])

  const downloadAttachmentMutation = useMutation({
    mutationFn: async (attachmentId: string) => {
      const payload = await coursesApi.presignDownloadAttachment(attachmentId)
      await downloadPresignedFile(payload)
    },
  })

  const saveMutation = useMutation({
    mutationFn: async () => {
      const m = materialQuery.data!
      const courseIdForUpload = m.courseId

      const uploadedIds: string[] = []
      for (const file of newFiles) {
        const presign = await coursesApi.presignUploadAttachment({
          courseId: courseIdForUpload,
          fileName: file.name,
          contentType: file.type || undefined,
          fileSize: file.size,
        })
        await uploadFileToPresignedUrl(presign, file)
        uploadedIds.push(presign.attachmentId)
      }

      return coursesApi.updateMaterial(materialId!, {
        title: title.trim(),
        description: description.trim() || undefined,
        body: body.trim() || undefined,
        position: m.position,
        isVisible,
        clearBlock: !m.blockId,
        blockId: m.blockId ?? undefined,
        attachmentIds: [...existingAttachments.map((a) => a.attachmentId), ...uploadedIds],
      })
    },
    onSuccess: () => {
      setNewFiles([])
      setIsEditing(false)
      queryClient.invalidateQueries({ queryKey: ['material', materialId] })
      if (courseId) {
        queryClient.invalidateQueries({ queryKey: ['course-materials', courseId] })
      }
    },
  })

  const downloadError = downloadAttachmentMutation.isError
    ? getErrorMessage(downloadAttachmentMutation.error, 'Не удалось скачать файл')
    : null

  if (!courseId || !materialId) {
    return (
      <EmptyState
        title="Материал не найден"
        description="В адресе отсутствует идентификатор материала."
      />
    )
  }

  if (materialQuery.isPending || membershipQuery.isPending) {
    return <PageLoader label="Загружаем материал..." />
  }

  if (materialQuery.isError) {
    return (
      <EmptyState
        title="Не удалось загрузить материал"
        description={getErrorMessage(materialQuery.error)}
      />
    )
  }

  const material = materialQuery.data

  function cancelEditing() {
    setTitle(material.title)
    setDescription(material.description ?? '')
    setBody(material.body ?? '')
    setIsVisible(material.isVisible)
    setExistingAttachments(material.attachments)
    setNewFiles([])
    setIsEditing(false)
  }

  return (
    <div className="space-y-8">
      {downloadError ? (
        <div className="rounded-2xl border border-rose-500/30 bg-rose-500/10 px-4 py-3 text-sm text-rose-200">
          {downloadError}
        </div>
      ) : null}

      <section className="rounded-3xl border border-slate-800 bg-slate-950/70 p-8">
        <div className="flex flex-wrap items-start justify-between gap-4">
          <Link className="text-sm text-blue-300 hover:text-blue-200" to={`/app/courses/${courseId}`}>
            ← Назад к курсу
          </Link>
          {canEditMaterial && !isEditing ? (
            <Button variant="secondary" onClick={() => setIsEditing(true)}>
              Редактировать
            </Button>
          ) : null}
        </div>

        {isEditing ? (
          <div className="mt-4 space-y-4">
            <Input
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="Название материала"
            />
            <Input
              value={description}
              onChange={(e) => setDescription(e.target.value)}
              placeholder="Краткое описание (необязательно)"
            />
            <label className="flex items-center gap-2 text-sm text-slate-300">
              <input
                type="checkbox"
                checked={isVisible}
                onChange={(e) => setIsVisible(e.target.checked)}
              />
              Виден студентам
            </label>
          </div>
        ) : (
          <>
            <h1 className="mt-4 text-3xl font-semibold text-white">{material.title}</h1>
            <p className="mt-3 text-sm text-slate-400">
              {material.description || 'Краткое описание отсутствует.'}
            </p>
            <div className="mt-6 flex flex-wrap gap-2 text-xs text-slate-400">
              <span className="rounded-full border border-slate-700 px-3 py-1">
                {material.isVisible ? 'Виден студентам' : 'Скрыт'}
              </span>
              <span className="rounded-full border border-slate-700 px-3 py-1">
                Обновлен: {formatDateTime(material.updatedAt)}
              </span>
            </div>
          </>
        )}
      </section>

      <section className="grid gap-6 xl:grid-cols-[1.2fr_0.8fr]">
        <article className="rounded-3xl border border-slate-800 bg-slate-950/70 p-8">
          <h2 className="text-xl font-semibold text-white">Содержимое</h2>
          {isEditing ? (
            <Textarea
              className="mt-5"
              rows={16}
              value={body}
              onChange={(e) => setBody(e.target.value)}
              placeholder="Текст материала..."
            />
          ) : (
            <div className="mt-5 whitespace-pre-wrap text-sm leading-7 text-slate-200">
              {material.body || 'Тело материала пока пустое.'}
            </div>
          )}
        </article>

        <aside className="rounded-3xl border border-slate-800 bg-slate-950/70 p-6">
          <h2 className="text-xl font-semibold text-white">Вложения</h2>

          {isEditing ? (
            <div className="mt-4 space-y-3">
              {existingAttachments.map((attachment) => (
                <div
                  key={attachment.attachmentId}
                  className="flex items-center justify-between rounded-2xl border border-slate-800 bg-slate-900/70 p-4"
                >
                  <div>
                    <div className="text-sm font-medium text-white">{attachment.fileName}</div>
                    <div className="mt-1 text-xs text-slate-500">
                      {formatFileSize(attachment.fileSize)}
                    </div>
                  </div>
                  <button
                    className="text-xs text-slate-500 transition hover:text-rose-400"
                    onClick={() =>
                      setExistingAttachments((prev) =>
                        prev.filter((a) => a.attachmentId !== attachment.attachmentId),
                      )
                    }
                  >
                    Убрать
                  </button>
                </div>
              ))}

              {newFiles.map((file, i) => (
                <div
                  key={i}
                  className="flex items-center justify-between rounded-2xl border border-blue-500/20 bg-blue-500/5 p-4"
                >
                  <div>
                    <div className="text-sm font-medium text-blue-200">{file.name}</div>
                    <div className="mt-1 text-xs text-slate-500">
                      {formatFileSize(file.size)} · новый
                    </div>
                  </div>
                  <button
                    className="text-xs text-slate-500 transition hover:text-rose-400"
                    onClick={() => setNewFiles((prev) => prev.filter((_, j) => j !== i))}
                  >
                    Убрать
                  </button>
                </div>
              ))}

              <div className="space-y-2">
                <span className="text-sm text-slate-300">Добавить файлы</span>
                <Input
                  type="file"
                  multiple
                  onChange={(e: ChangeEvent<HTMLInputElement>) => {
                    const list = e.target.files
                    if (list?.length) {
                      const picked = Array.from(list)
                      setNewFiles((prev) => [...prev, ...picked])
                    }
                    e.target.value = ''
                  }}
                />
              </div>
            </div>
          ) : material.attachments.length === 0 ? (
            <p className="mt-4 text-sm text-slate-400">У материала пока нет файлов.</p>
          ) : (
            <div className="mt-4 space-y-3">
              {material.attachments.map((attachment) => (
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
        </aside>
      </section>

      {isEditing ? (
        <div className="flex flex-wrap gap-3">
          <Button
            onClick={() => saveMutation.mutate()}
            isLoading={saveMutation.isPending}
          >
            Сохранить изменения
          </Button>
          <Button variant="secondary" onClick={cancelEditing}>
            Отмена
          </Button>
          {saveMutation.isError ? (
            <div className="w-full rounded-2xl border border-rose-500/30 bg-rose-500/10 px-4 py-3 text-sm text-rose-200">
              {getErrorMessage(saveMutation.error, 'Не удалось сохранить')}
            </div>
          ) : null}
        </div>
      ) : null}
    </div>
  )
}
