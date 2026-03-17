import type {
  CodeAttemptLogDownloadResponse,
  PresignDownloadAttachmentResponse,
  PresignUploadAttachmentResponse,
} from '@/entities/course/types'

export async function uploadFileToPresignedUrl(
  presign: PresignUploadAttachmentResponse,
  file: File,
) {
  const response = await fetch(presign.uploadUrl, {
    method: presign.method,
    headers: file.type
      ? {
          'Content-Type': file.type,
        }
      : undefined,
    body: file,
  })

  if (!response.ok) {
    throw new Error(`Не удалось загрузить файл "${file.name}"`)
  }
}

export async function downloadPresignedFile(
  payload: PresignDownloadAttachmentResponse | CodeAttemptLogDownloadResponse,
) {
  const response = await fetch(payload.downloadUrl, {
    method: payload.method,
  })

  if (!response.ok) {
    throw new Error(`Не удалось скачать "${payload.fileName}"`)
  }

  const blob = await response.blob()
  const objectUrl = URL.createObjectURL(blob)
  const link = document.createElement('a')

  link.href = objectUrl
  link.download = payload.fileName
  document.body.append(link)
  link.click()
  link.remove()

  URL.revokeObjectURL(objectUrl)
}
