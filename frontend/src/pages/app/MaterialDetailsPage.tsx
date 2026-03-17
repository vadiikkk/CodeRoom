import { useMutation, useQuery } from '@tanstack/react-query'
import { Link, useParams } from 'react-router-dom'

import { coursesApi } from '@/shared/api/courses'
import { downloadPresignedFile } from '@/shared/lib/attachments'
import { getErrorMessage } from '@/shared/lib/errors'
import { formatDateTime, formatFileSize } from '@/shared/lib/format'
import { Button } from '@/shared/ui/Button'
import { EmptyState } from '@/shared/ui/EmptyState'
import { PageLoader } from '@/shared/ui/PageLoader'

export function MaterialDetailsPage() {
  const params = useParams<{ courseId: string; materialId: string }>()
  const courseId = params.courseId
  const materialId = params.materialId

  const materialQuery = useQuery({
    queryKey: ['material', materialId],
    queryFn: () => coursesApi.getMaterial(materialId!),
    enabled: Boolean(materialId),
  })

  const downloadAttachmentMutation = useMutation({
    mutationFn: async (attachmentId: string) => {
      const payload = await coursesApi.presignDownloadAttachment(attachmentId)
      await downloadPresignedFile(payload)
    },
  })

  if (!courseId || !materialId) {
    return (
      <EmptyState
        title="Материал не найден"
        description="В адресе отсутствует идентификатор материала."
      />
    )
  }

  if (materialQuery.isPending) {
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

  return (
    <div className="space-y-8">
      <section className="rounded-3xl border border-slate-800 bg-slate-950/70 p-8">
        <Link className="text-sm text-blue-300 hover:text-blue-200" to={`/app/courses/${courseId}`}>
          ← Назад к курсу
        </Link>
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
      </section>

      <section className="grid gap-6 xl:grid-cols-[1.2fr_0.8fr]">
        <article className="rounded-3xl border border-slate-800 bg-slate-950/70 p-8">
          <h2 className="text-xl font-semibold text-white">Содержимое</h2>
          <div className="mt-5 whitespace-pre-wrap text-sm leading-7 text-slate-200">
            {material.body || 'Тело материала пока пустое.'}
          </div>
        </article>

        <aside className="rounded-3xl border border-slate-800 bg-slate-950/70 p-6">
          <h2 className="text-xl font-semibold text-white">Вложения</h2>
          {material.attachments.length === 0 ? (
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
    </div>
  )
}
