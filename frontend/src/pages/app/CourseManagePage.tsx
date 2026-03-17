import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useEffect, useMemo, useState, type ChangeEvent, type ReactNode } from 'react'
import { Link, useParams } from 'react-router-dom'

import type {
  AssignmentResponse,
  AssignmentType,
  AttachmentResponse,
  BlockResponse,
  CreateAssignmentRequest,
  CreateMaterialRequest,
  MaterialResponse,
} from '@/entities/course/types'
import { coursesApi } from '@/shared/api/courses'
import { uploadFileToPresignedUrl } from '@/shared/lib/attachments'
import { getErrorMessage } from '@/shared/lib/errors'
import { formatDateTime, formatFileSize } from '@/shared/lib/format'
import { Button } from '@/shared/ui/Button'
import { EmptyState } from '@/shared/ui/EmptyState'
import { Input, Textarea } from '@/shared/ui/Input'
import { PageLoader } from '@/shared/ui/PageLoader'

type MaterialDraft = {
  title: string
  description: string
  body: string
  blockId: string
  position: string
  isVisible: boolean
  files: File[]
  existingAttachments: AttachmentResponse[]
}

type AssignmentDraft = {
  title: string
  description: string
  assignmentType: AssignmentType
  workType: 'INDIVIDUAL' | 'GROUP'
  deadlineAt: string
  weight: string
  blockId: string
  position: string
  isVisible: boolean
  files: File[]
  existingAttachments: AttachmentResponse[]
  repositoryName: string
  repositoryDescription: string
  language: 'GO' | 'PYTHON' | 'JAVA'
  maxAttempts: string
  privateTestsFile: File | null
  hasExistingPrivateTests: boolean
  clearPrivateTests: boolean
  githubPatOverride: string
}

const emptyMaterialDraft = (): MaterialDraft => ({
  title: '',
  description: '',
  body: '',
  blockId: '',
  position: '10',
  isVisible: true,
  files: [],
  existingAttachments: [],
})

const emptyAssignmentDraft = (): AssignmentDraft => ({
  title: '',
  description: '',
  assignmentType: 'TEXT',
  workType: 'INDIVIDUAL',
  deadlineAt: '',
  weight: '0.3',
  blockId: '',
  position: '10',
  isVisible: false,
  files: [],
  existingAttachments: [],
  repositoryName: '',
  repositoryDescription: '',
  language: 'GO',
  maxAttempts: '3',
  privateTestsFile: null,
  hasExistingPrivateTests: false,
  clearPrivateTests: false,
  githubPatOverride: '',
})

export function CourseManagePage() {
  const params = useParams<{ courseId: string }>()
  const courseId = params.courseId
  const queryClient = useQueryClient()
  const [courseForm, setCourseForm] = useState({
    title: '',
    description: '',
    isVisible: false,
  })
  const [patValue, setPatValue] = useState('')
  const [newBlockTitle, setNewBlockTitle] = useState('')
  const [newBlockPosition, setNewBlockPosition] = useState('10')
  const [editingMaterialId, setEditingMaterialId] = useState<string | null>(null)
  const [materialDraft, setMaterialDraft] = useState<MaterialDraft>(emptyMaterialDraft())
  const [editingAssignmentId, setEditingAssignmentId] = useState<string | null>(null)
  const [assignmentDraft, setAssignmentDraft] = useState<AssignmentDraft>(emptyAssignmentDraft())

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

  const structureQuery = useQuery({
    queryKey: ['course-structure', courseId],
    queryFn: () => coursesApi.getStructure(courseId!),
    enabled: Boolean(courseId),
  })

  const materialsQuery = useQuery({
    queryKey: ['course-materials', courseId],
    queryFn: () => coursesApi.listMaterials(courseId!),
    enabled: Boolean(courseId),
  })

  const assignmentsQuery = useQuery({
    queryKey: ['course-assignments', courseId],
    queryFn: () => coursesApi.listAssignments(courseId!),
    enabled: Boolean(courseId),
  })

  const githubPatQuery = useQuery({
    queryKey: ['course-github-pat', courseId],
    queryFn: () => coursesApi.getGithubPatStatus(courseId!),
    enabled: Boolean(courseId),
  })

  useEffect(() => {
    if (!courseQuery.data) {
      return
    }

    setCourseForm({
      title: courseQuery.data.title,
      description: courseQuery.data.description ?? '',
      isVisible: courseQuery.data.isVisible,
    })
  }, [courseQuery.data])

  const blocks = useMemo(() => structureQuery.data?.blocks.map((entry) => entry.block) ?? [], [
    structureQuery.data,
  ])

  const invalidateCourseData = async () => {
    await Promise.all([
      queryClient.invalidateQueries({ queryKey: ['course', courseId] }),
      queryClient.invalidateQueries({ queryKey: ['course-structure', courseId] }),
      queryClient.invalidateQueries({ queryKey: ['course-materials', courseId] }),
      queryClient.invalidateQueries({ queryKey: ['course-assignments', courseId] }),
      queryClient.invalidateQueries({ queryKey: ['course-github-pat', courseId] }),
    ])
  }

  const updateCourseMutation = useMutation({
    mutationFn: () =>
      coursesApi.updateCourse(courseId!, {
        title: courseForm.title.trim(),
        description: courseForm.description.trim() || undefined,
        isVisible: courseForm.isVisible,
      }),
    onSuccess: async (course) => {
      setCourseForm({
        title: course.title,
        description: course.description ?? '',
        isVisible: course.isVisible,
      })
      await invalidateCourseData()
    },
  })

  const setGithubPatMutation = useMutation({
    mutationFn: () =>
      coursesApi.setGithubPat(courseId!, {
        token: patValue.trim(),
      }),
    onSuccess: async () => {
      setPatValue('')
      await invalidateCourseData()
    },
  })

  const clearGithubPatMutation = useMutation({
    mutationFn: () => coursesApi.clearGithubPat(courseId!),
    onSuccess: invalidateCourseData,
  })

  const createBlockMutation = useMutation({
    mutationFn: () =>
      coursesApi.createBlock(courseId!, {
        title: newBlockTitle.trim(),
        position: Number(newBlockPosition),
        isVisible: true,
      }),
    onSuccess: async () => {
      setNewBlockTitle('')
      setNewBlockPosition('10')
      await invalidateCourseData()
    },
  })

  const updateBlockMutation = useMutation({
    mutationFn: ({
      blockId,
      title,
      position,
      isVisible,
    }: {
      blockId: string
      title: string
      position: number
      isVisible: boolean
    }) =>
      coursesApi.updateBlock(courseId!, blockId, {
        title,
        position,
        isVisible,
      }),
    onSuccess: invalidateCourseData,
  })

  const deleteBlockMutation = useMutation({
    mutationFn: (blockId: string) => coursesApi.deleteBlock(courseId!, blockId),
    onSuccess: invalidateCourseData,
  })

  const saveMaterialMutation = useMutation({
    mutationFn: async () => {
      const attachmentIds = [
        ...materialDraft.existingAttachments.map((attachment) => attachment.attachmentId),
        ...(await uploadFiles(courseId!, materialDraft.files)),
      ]

      if (editingMaterialId) {
        return coursesApi.updateMaterial(editingMaterialId, {
          title: materialDraft.title.trim(),
          description: materialDraft.description.trim() || undefined,
          body: materialDraft.body.trim() || undefined,
          blockId: materialDraft.blockId || undefined,
          clearBlock: !materialDraft.blockId,
          position: Number(materialDraft.position),
          isVisible: materialDraft.isVisible,
          attachmentIds,
        })
      }

      const payload: CreateMaterialRequest = {
        title: materialDraft.title.trim(),
        description: materialDraft.description.trim() || undefined,
        body: materialDraft.body.trim() || undefined,
        blockId: materialDraft.blockId || undefined,
        position: Number(materialDraft.position),
        isVisible: materialDraft.isVisible,
        attachmentIds,
      }

      return coursesApi.createMaterial(courseId!, payload)
    },
    onSuccess: async () => {
      setEditingMaterialId(null)
      setMaterialDraft(emptyMaterialDraft())
      await invalidateCourseData()
    },
  })

  const deleteMaterialMutation = useMutation({
    mutationFn: (materialId: string) => coursesApi.deleteMaterial(materialId),
    onSuccess: invalidateCourseData,
  })

  const saveAssignmentMutation = useMutation({
    mutationFn: async () => {
      const attachmentIds = [
        ...assignmentDraft.existingAttachments.map((attachment) => attachment.attachmentId),
        ...(await uploadFiles(courseId!, assignmentDraft.files)),
      ]

      const deadlineAt = assignmentDraft.deadlineAt
        ? new Date(assignmentDraft.deadlineAt).toISOString()
        : undefined

      if (editingAssignmentId) {
        const existingAssignment = assignmentsQuery.data?.find(
          (assignment) => assignment.assignmentId === editingAssignmentId,
        )

        return coursesApi.updateAssignment(editingAssignmentId, {
          title: assignmentDraft.title.trim(),
          description: assignmentDraft.description.trim() || undefined,
          assignmentType: existingAssignment?.assignmentType,
          workType: assignmentDraft.workType,
          deadlineAt,
          clearDeadline: !deadlineAt,
          weight: Number(assignmentDraft.weight),
          blockId: assignmentDraft.blockId || undefined,
          clearBlock: !assignmentDraft.blockId,
          position: Number(assignmentDraft.position),
          isVisible: assignmentDraft.isVisible,
          attachmentIds,
          code:
            existingAssignment?.assignmentType === 'CODE'
              ? {
                  maxAttempts: Number(assignmentDraft.maxAttempts),
                  privateTestsAttachmentId: assignmentDraft.privateTestsFile
                    ? (await uploadFiles(courseId!, [assignmentDraft.privateTestsFile]))[0]
                    : undefined,
                  clearPrivateTestsAttachment: assignmentDraft.clearPrivateTests,
                }
              : undefined,
        })
      }

      const payload: CreateAssignmentRequest = {
        title: assignmentDraft.title.trim(),
        description: assignmentDraft.description.trim() || undefined,
        assignmentType: assignmentDraft.assignmentType,
        workType: assignmentDraft.workType,
        deadlineAt,
        weight: Number(assignmentDraft.weight),
        blockId: assignmentDraft.blockId || undefined,
        position: Number(assignmentDraft.position),
        isVisible: assignmentDraft.isVisible,
        attachmentIds,
        code:
          assignmentDraft.assignmentType === 'CODE'
            ? {
                repositoryName: assignmentDraft.repositoryName.trim(),
                repositoryDescription:
                  assignmentDraft.repositoryDescription.trim() || undefined,
                language: assignmentDraft.language,
                maxAttempts: Number(assignmentDraft.maxAttempts),
                privateTestsAttachmentId: assignmentDraft.privateTestsFile
                  ? (await uploadFiles(courseId!, [assignmentDraft.privateTestsFile]))[0]
                  : undefined,
                githubPat: assignmentDraft.githubPatOverride.trim() || undefined,
              }
            : undefined,
      }

      return coursesApi.createAssignment(courseId!, payload)
    },
    onSuccess: async () => {
      setEditingAssignmentId(null)
      setAssignmentDraft(emptyAssignmentDraft())
      await invalidateCourseData()
    },
  })

  const deleteAssignmentMutation = useMutation({
    mutationFn: (assignmentId: string) => coursesApi.deleteAssignment(assignmentId),
    onSuccess: invalidateCourseData,
  })

  const publishAssignmentMutation = useMutation({
    mutationFn: (assignmentId: string) => coursesApi.publishAssignment(assignmentId),
    onSuccess: invalidateCourseData,
  })

  if (!courseId) {
    return (
      <EmptyState
        title="Курс не найден"
        description="В адресе отсутствует идентификатор курса."
      />
    )
  }

  if (
    courseQuery.isPending ||
    membershipQuery.isPending ||
    structureQuery.isPending ||
    materialsQuery.isPending ||
    assignmentsQuery.isPending ||
    githubPatQuery.isPending
  ) {
    return <PageLoader label="Загружаем панель курса..." />
  }

  const firstError = [
    courseQuery.error,
    membershipQuery.error,
    structureQuery.error,
    materialsQuery.error,
    assignmentsQuery.error,
    githubPatQuery.error,
  ].find(Boolean)

  if (firstError) {
    return (
      <EmptyState
        title="Не удалось открыть управление курсом"
        description={getErrorMessage(firstError)}
      />
    )
  }

  const course = courseQuery.data!
  const membership = membershipQuery.data!
  const materials = materialsQuery.data!
  const assignments = assignmentsQuery.data!
  const githubPatStatus = githubPatQuery.data!

  if (membership.roleInCourse !== 'TEACHER') {
    return (
      <EmptyState
        title="Недостаточно прав"
        description="Управление курсом доступно только преподавателю курса."
      />
    )
  }

  return (
    <div className="space-y-8">
      <section className="rounded-3xl border border-slate-800 bg-slate-950/70 p-8">
        <div className="flex flex-col gap-5 lg:flex-row lg:items-start lg:justify-between">
          <div>
            <Link className="text-sm text-blue-300 hover:text-blue-200" to={`/app/courses/${courseId}`}>
              ← Вернуться к курсу
            </Link>
            <h1 className="mt-4 text-3xl font-semibold text-white">Управление курсом</h1>
            <p className="mt-3 max-w-3xl text-sm text-slate-400">
              Настраивайте параметры курса, структуру, материалы и задания в одном месте.
            </p>
          </div>
          <div className="flex flex-wrap gap-2 text-xs text-slate-300">
            <span className="rounded-full border border-slate-700 px-3 py-1">
              {course.title}
            </span>
            <span className="rounded-full border border-slate-700 px-3 py-1">
              {course.isVisible ? 'Опубликован' : 'Скрыт'}
            </span>
          </div>
        </div>
      </section>

      <section className="grid gap-6 xl:grid-cols-[1.1fr_0.9fr]">
        <PanelCard title="Настройки курса">
          <div className="space-y-4">
            <Field label="Название">
              <Input
                value={courseForm.title}
                onChange={(event) =>
                  setCourseForm((prev) => ({ ...prev, title: event.target.value }))
                }
              />
            </Field>
            <Field label="Описание">
              <Textarea
                rows={5}
                value={courseForm.description}
                onChange={(event) =>
                  setCourseForm((prev) => ({ ...prev, description: event.target.value }))
                }
              />
            </Field>
            <CheckboxRow
              label="Курс виден студентам"
              checked={courseForm.isVisible}
              onChange={(checked) =>
                setCourseForm((prev) => ({ ...prev, isVisible: checked }))
              }
            />
            {updateCourseMutation.isError ? (
              <InlineError message={getErrorMessage(updateCourseMutation.error)} />
            ) : null}
            <Button onClick={() => updateCourseMutation.mutate()} isLoading={updateCourseMutation.isPending}>
              Сохранить настройки
            </Button>
          </div>
        </PanelCard>

        <PanelCard title="GitHub PAT">
          <div className="space-y-4">
            <p className="text-sm text-slate-400">
              Токен нужен для code assignments и интеграции с GitHub-репозиториями.
            </p>
            <div className="rounded-2xl border border-slate-800 bg-slate-900/70 p-4 text-sm text-slate-300">
              {githubPatStatus.configured
                ? `Токен настроен${githubPatStatus.updatedAt ? ` · обновлен ${formatDateTime(githubPatStatus.updatedAt)}` : ''}`
                : 'Токен пока не настроен'}
            </div>
            <Field label="Новый токен">
              <Input
                type="password"
                value={patValue}
                onChange={(event) => setPatValue(event.target.value)}
                placeholder="github_pat_..."
              />
            </Field>
            {setGithubPatMutation.isError ? (
              <InlineError message={getErrorMessage(setGithubPatMutation.error)} />
            ) : null}
            <div className="flex flex-wrap gap-3">
              <Button
                onClick={() => setGithubPatMutation.mutate()}
                isLoading={setGithubPatMutation.isPending}
              >
                Сохранить токен
              </Button>
              {githubPatStatus.configured ? (
                <Button
                  variant="secondary"
                  onClick={() => clearGithubPatMutation.mutate()}
                  isLoading={clearGithubPatMutation.isPending}
                >
                  Очистить токен
                </Button>
              ) : null}
            </div>
          </div>
        </PanelCard>
      </section>

      <section className="grid gap-6 xl:grid-cols-[0.9fr_1.1fr]">
        <PanelCard title="Блоки курса">
          <div className="space-y-5">
            <div className="grid gap-3 md:grid-cols-[1fr_140px_auto]">
              <Input
                value={newBlockTitle}
                onChange={(event) => setNewBlockTitle(event.target.value)}
                placeholder="Название блока"
              />
              <Input
                type="number"
                value={newBlockPosition}
                onChange={(event) => setNewBlockPosition(event.target.value)}
                placeholder="Позиция"
              />
              <Button
                onClick={() => createBlockMutation.mutate()}
                isLoading={createBlockMutation.isPending}
              >
                Добавить блок
              </Button>
            </div>

            {blocks.length === 0 ? (
              <p className="text-sm text-slate-400">Блоков пока нет.</p>
            ) : (
              <div className="space-y-4">
                {blocks
                  .slice()
                  .sort((left, right) => left.position - right.position)
                  .map((block) => (
                    <BlockEditor
                      key={block.blockId}
                      block={block}
                      onSave={(payload) => updateBlockMutation.mutate(payload)}
                      onDelete={(blockId) => deleteBlockMutation.mutate(blockId)}
                    />
                  ))}
              </div>
            )}
          </div>
        </PanelCard>

        <PanelCard title={editingMaterialId ? 'Редактирование материала' : 'Новый материал'}>
          <div className="space-y-4">
            <Field label="Название">
              <Input
                value={materialDraft.title}
                onChange={(event) =>
                  setMaterialDraft((prev) => ({ ...prev, title: event.target.value }))
                }
              />
            </Field>
            <Field label="Краткое описание">
              <Textarea
                rows={3}
                value={materialDraft.description}
                onChange={(event) =>
                  setMaterialDraft((prev) => ({ ...prev, description: event.target.value }))
                }
              />
            </Field>
            <Field label="Содержимое">
              <Textarea
                rows={10}
                value={materialDraft.body}
                onChange={(event) =>
                  setMaterialDraft((prev) => ({ ...prev, body: event.target.value }))
                }
              />
            </Field>
            <div className="grid gap-4 md:grid-cols-2">
              <Field label="Блок">
                <SelectField
                  value={materialDraft.blockId}
                  onChange={(value) =>
                    setMaterialDraft((prev) => ({ ...prev, blockId: value }))
                  }
                  options={buildBlockOptions(blocks)}
                />
              </Field>
              <Field label="Позиция">
                <Input
                  type="number"
                  value={materialDraft.position}
                  onChange={(event) =>
                    setMaterialDraft((prev) => ({ ...prev, position: event.target.value }))
                  }
                />
              </Field>
            </div>
            <CheckboxRow
              label="Материал виден студентам"
              checked={materialDraft.isVisible}
              onChange={(checked) =>
                setMaterialDraft((prev) => ({ ...prev, isVisible: checked }))
              }
            />
            <Field label="Файлы">
              <Input
                type="file"
                multiple
                onChange={(event: ChangeEvent<HTMLInputElement>) =>
                  setMaterialDraft((prev) => ({
                    ...prev,
                    files: Array.from(event.target.files ?? []),
                  }))
                }
              />
            </Field>
            <AttachmentList title="Текущие вложения" attachments={materialDraft.existingAttachments} />
            {saveMaterialMutation.isError ? (
              <InlineError message={getErrorMessage(saveMaterialMutation.error)} />
            ) : null}
            <div className="flex flex-wrap gap-3">
              <Button
                onClick={() => saveMaterialMutation.mutate()}
                isLoading={saveMaterialMutation.isPending}
              >
                {editingMaterialId ? 'Сохранить материал' : 'Создать материал'}
              </Button>
              {editingMaterialId ? (
                <Button
                  variant="secondary"
                  onClick={() => {
                    setEditingMaterialId(null)
                    setMaterialDraft(emptyMaterialDraft())
                  }}
                >
                  Отменить
                </Button>
              ) : null}
            </div>
          </div>
        </PanelCard>
      </section>

      <PanelCard title="Материалы курса">
        <RecordList
          emptyText="Материалов пока нет."
          items={materials
            .slice()
            .sort((left, right) => left.position - right.position)
            .map((material) => ({
              id: material.materialId,
              title: material.title,
              description: material.description || material.body || 'Без описания',
              meta: [
                material.blockId ? `Блок: ${getBlockTitle(blocks, material.blockId)}` : 'Без блока',
                `Позиция: ${material.position}`,
                material.isVisible ? 'Виден студентам' : 'Скрыт',
              ],
              onEdit: () => {
                setEditingMaterialId(material.materialId)
                setMaterialDraft(buildMaterialDraft(material))
              },
              onDelete: () => deleteMaterialMutation.mutate(material.materialId),
            }))}
        />
      </PanelCard>

      <section className="grid gap-6 xl:grid-cols-[1.1fr_0.9fr]">
        <PanelCard title={editingAssignmentId ? 'Редактирование задания' : 'Новое задание'}>
          <div className="space-y-4">
            <div className="grid gap-4 md:grid-cols-2">
              <Field label="Название">
                <Input
                  value={assignmentDraft.title}
                  onChange={(event) =>
                    setAssignmentDraft((prev) => ({ ...prev, title: event.target.value }))
                  }
                />
              </Field>
              <Field label="Тип задания">
                <SelectField
                  disabled={Boolean(editingAssignmentId)}
                  value={assignmentDraft.assignmentType}
                  onChange={(value) =>
                    setAssignmentDraft((prev) => ({
                      ...prev,
                      assignmentType: value as AssignmentType,
                    }))
                  }
                  options={[
                    { value: 'TEXT', label: 'Текстовое' },
                    { value: 'FILE', label: 'Файловое' },
                    { value: 'CODE', label: 'Кодовое' },
                  ]}
                />
              </Field>
            </div>

            <Field label="Описание">
              <Textarea
                rows={5}
                value={assignmentDraft.description}
                onChange={(event) =>
                  setAssignmentDraft((prev) => ({ ...prev, description: event.target.value }))
                }
              />
            </Field>

            <div className="grid gap-4 md:grid-cols-2">
              <Field label="Формат работы">
                <SelectField
                  value={assignmentDraft.workType}
                  onChange={(value) =>
                    setAssignmentDraft((prev) => ({
                      ...prev,
                      workType: value as 'INDIVIDUAL' | 'GROUP',
                    }))
                  }
                  options={[
                    { value: 'INDIVIDUAL', label: 'Индивидуальная работа' },
                    { value: 'GROUP', label: 'Групповая работа' },
                  ]}
                />
              </Field>
              <Field label="Вес">
                <Input
                  type="number"
                  step="0.1"
                  min="0"
                  max="1"
                  value={assignmentDraft.weight}
                  onChange={(event) =>
                    setAssignmentDraft((prev) => ({ ...prev, weight: event.target.value }))
                  }
                />
              </Field>
              <Field label="Блок">
                <SelectField
                  value={assignmentDraft.blockId}
                  onChange={(value) =>
                    setAssignmentDraft((prev) => ({ ...prev, blockId: value }))
                  }
                  options={buildBlockOptions(blocks)}
                />
              </Field>
              <Field label="Позиция">
                <Input
                  type="number"
                  value={assignmentDraft.position}
                  onChange={(event) =>
                    setAssignmentDraft((prev) => ({ ...prev, position: event.target.value }))
                  }
                />
              </Field>
              <Field label="Дедлайн">
                <Input
                  type="datetime-local"
                  value={assignmentDraft.deadlineAt}
                  onChange={(event) =>
                    setAssignmentDraft((prev) => ({ ...prev, deadlineAt: event.target.value }))
                  }
                />
              </Field>
            </div>

            <CheckboxRow
              label="Задание видно студентам"
              checked={assignmentDraft.isVisible}
              onChange={(checked) =>
                setAssignmentDraft((prev) => ({ ...prev, isVisible: checked }))
              }
            />

            {assignmentDraft.assignmentType === 'CODE' ? (
              <div className="rounded-2xl border border-slate-800 bg-slate-900/70 p-5">
                <h3 className="text-sm font-semibold text-white">Настройки code assignment</h3>
                <div className="mt-4 grid gap-4 md:grid-cols-2">
                  <Field label="Имя репозитория">
                    <Input
                      disabled={Boolean(editingAssignmentId)}
                      value={assignmentDraft.repositoryName}
                      onChange={(event) =>
                        setAssignmentDraft((prev) => ({
                          ...prev,
                          repositoryName: event.target.value,
                        }))
                      }
                    />
                  </Field>
                  <Field label="Язык">
                    <SelectField
                      disabled={Boolean(editingAssignmentId)}
                      value={assignmentDraft.language}
                      onChange={(value) =>
                        setAssignmentDraft((prev) => ({
                          ...prev,
                          language: value as 'GO' | 'PYTHON' | 'JAVA',
                        }))
                      }
                      options={[
                        { value: 'GO', label: 'Go' },
                        { value: 'PYTHON', label: 'Python' },
                        { value: 'JAVA', label: 'Java' },
                      ]}
                    />
                  </Field>
                  <Field label="Описание репозитория">
                    <Input
                      disabled={Boolean(editingAssignmentId)}
                      value={assignmentDraft.repositoryDescription}
                      onChange={(event) =>
                        setAssignmentDraft((prev) => ({
                          ...prev,
                          repositoryDescription: event.target.value,
                        }))
                      }
                    />
                  </Field>
                  <Field label="Максимум попыток">
                    <Input
                      type="number"
                      min="1"
                      value={assignmentDraft.maxAttempts}
                      onChange={(event) =>
                        setAssignmentDraft((prev) => ({
                          ...prev,
                          maxAttempts: event.target.value,
                        }))
                      }
                    />
                  </Field>
                </div>
                {!editingAssignmentId ? (
                  <Field label="PAT только для этого задания">
                    <Input
                      type="password"
                      value={assignmentDraft.githubPatOverride}
                      onChange={(event) =>
                        setAssignmentDraft((prev) => ({
                          ...prev,
                          githubPatOverride: event.target.value,
                        }))
                      }
                    />
                  </Field>
                ) : null}
                <Field label="Файл private tests">
                  <Input
                    type="file"
                    onChange={(event: ChangeEvent<HTMLInputElement>) =>
                      setAssignmentDraft((prev) => ({
                        ...prev,
                        privateTestsFile: event.target.files?.[0] ?? null,
                      }))
                    }
                  />
                </Field>
                {editingAssignmentId && assignmentDraft.hasExistingPrivateTests ? (
                  <CheckboxRow
                    label="Удалить текущий архив private tests"
                    checked={assignmentDraft.clearPrivateTests}
                    onChange={(checked) =>
                      setAssignmentDraft((prev) => ({
                        ...prev,
                        clearPrivateTests: checked,
                      }))
                    }
                  />
                ) : null}
              </div>
            ) : (
              <>
                <Field label="Файлы задания">
                  <Input
                    type="file"
                    multiple
                    onChange={(event: ChangeEvent<HTMLInputElement>) =>
                      setAssignmentDraft((prev) => ({
                        ...prev,
                        files: Array.from(event.target.files ?? []),
                      }))
                    }
                  />
                </Field>
                <AttachmentList
                  title="Текущие вложения"
                  attachments={assignmentDraft.existingAttachments}
                />
              </>
            )}

            {saveAssignmentMutation.isError ? (
              <InlineError message={getErrorMessage(saveAssignmentMutation.error)} />
            ) : null}
            <div className="flex flex-wrap gap-3">
              <Button
                onClick={() => saveAssignmentMutation.mutate()}
                isLoading={saveAssignmentMutation.isPending}
              >
                {editingAssignmentId ? 'Сохранить задание' : 'Создать задание'}
              </Button>
              {editingAssignmentId ? (
                <Button
                  variant="secondary"
                  onClick={() => {
                    setEditingAssignmentId(null)
                    setAssignmentDraft(emptyAssignmentDraft())
                  }}
                >
                  Отменить
                </Button>
              ) : null}
            </div>
          </div>
        </PanelCard>

        <PanelCard title="Задания курса">
          <RecordList
            emptyText="Заданий пока нет."
            items={assignments
              .slice()
              .sort((left, right) => left.position - right.position)
              .map((assignment) => ({
                id: assignment.assignmentId,
                title: assignment.title,
                description: assignment.description || 'Без описания',
                meta: [
                  getAssignmentTypeName(assignment.assignmentType),
                  getWorkTypeName(assignment.workType),
                  assignment.blockId
                    ? `Блок: ${getBlockTitle(blocks, assignment.blockId)}`
                    : 'Без блока',
                  `Позиция: ${assignment.position}`,
                  assignment.isVisible ? 'Видно студентам' : 'Скрыто',
                ],
                extra:
                  assignment.assignmentType === 'CODE'
                    ? assignment.code?.publishedAt
                      ? `Опубликовано ${formatDateTime(assignment.code.publishedAt)}`
                      : 'Еще не опубликовано'
                    : undefined,
                onEdit: () => {
                  setEditingAssignmentId(assignment.assignmentId)
                  setAssignmentDraft(buildAssignmentDraft(assignment))
                },
                onDelete: () => deleteAssignmentMutation.mutate(assignment.assignmentId),
                action:
                  assignment.assignmentType === 'CODE' && !assignment.code?.publishedAt ? (
                    <Button
                      variant="secondary"
                      onClick={() => publishAssignmentMutation.mutate(assignment.assignmentId)}
                      isLoading={publishAssignmentMutation.isPending}
                    >
                      Опубликовать
                    </Button>
                  ) : undefined,
              }))}
          />
        </PanelCard>
      </section>
    </div>
  )
}

async function uploadFiles(courseId: string, files: File[]) {
  const attachmentIds: string[] = []

  for (const file of files) {
    const presign = await coursesApi.presignUploadAttachment({
      courseId,
      fileName: file.name,
      contentType: file.type || undefined,
      fileSize: file.size,
    })

    await uploadFileToPresignedUrl(presign, file)
    attachmentIds.push(presign.attachmentId)
  }

  return attachmentIds
}

function buildBlockOptions(blocks: BlockResponse[]) {
  return [
    { value: '', label: 'Без блока' },
    ...blocks
      .slice()
      .sort((left, right) => left.position - right.position)
      .map((block) => ({
        value: block.blockId,
        label: `${block.position}. ${block.title}`,
      })),
  ]
}

function getBlockTitle(blocks: BlockResponse[], blockId: string) {
  return blocks.find((block) => block.blockId === blockId)?.title ?? 'Без названия'
}

function buildMaterialDraft(material: MaterialResponse): MaterialDraft {
  return {
    title: material.title,
    description: material.description ?? '',
    body: material.body ?? '',
    blockId: material.blockId ?? '',
    position: String(material.position),
    isVisible: material.isVisible,
    files: [],
    existingAttachments: material.attachments,
  }
}

function buildAssignmentDraft(assignment: AssignmentResponse): AssignmentDraft {
  return {
    title: assignment.title,
    description: assignment.description ?? '',
    assignmentType: assignment.assignmentType,
    workType: assignment.workType,
    deadlineAt: assignment.deadlineAt ? toDateTimeLocalValue(assignment.deadlineAt) : '',
    weight: String(assignment.weight),
    blockId: assignment.blockId ?? '',
    position: String(assignment.position),
    isVisible: assignment.isVisible,
    files: [],
    existingAttachments: assignment.attachments,
    repositoryName: assignment.code?.repositoryName ?? '',
    repositoryDescription: '',
    language: assignment.code?.language ?? 'GO',
    maxAttempts: String(assignment.code?.maxAttempts ?? 3),
    privateTestsFile: null,
    hasExistingPrivateTests: Boolean(assignment.code?.privateTestsAttachment),
    clearPrivateTests: false,
    githubPatOverride: '',
  }
}

function toDateTimeLocalValue(value: string) {
  return new Date(value).toISOString().slice(0, 16)
}

function getAssignmentTypeName(type: AssignmentType) {
  switch (type) {
    case 'TEXT':
      return 'Текстовое'
    case 'FILE':
      return 'Файловое'
    case 'CODE':
      return 'Кодовое'
  }
}

function getWorkTypeName(type: 'INDIVIDUAL' | 'GROUP') {
  return type === 'GROUP' ? 'Групповое' : 'Индивидуальное'
}

function PanelCard({
  title,
  children,
}: {
  title: string
  children: ReactNode
}) {
  return (
    <section className="rounded-3xl border border-slate-800 bg-slate-950/70 p-6">
      <h2 className="text-xl font-semibold text-white">{title}</h2>
      <div className="mt-5">{children}</div>
    </section>
  )
}

function Field({
  label,
  children,
}: {
  label: string
  children: ReactNode
}) {
  return (
    <label className="block space-y-2">
      <span className="text-sm text-slate-300">{label}</span>
      {children}
    </label>
  )
}

function CheckboxRow({
  label,
  checked,
  onChange,
}: {
  label: string
  checked: boolean
  onChange: (checked: boolean) => void
}) {
  return (
    <label className="flex items-center gap-3 rounded-2xl border border-slate-800 bg-slate-900/60 px-4 py-3">
      <input
        type="checkbox"
        checked={checked}
        onChange={(event) => onChange(event.target.checked)}
      />
      <span className="text-sm text-slate-200">{label}</span>
    </label>
  )
}

function SelectField({
  value,
  onChange,
  options,
  disabled = false,
}: {
  value: string
  onChange: (value: string) => void
  options: Array<{ value: string; label: string }>
  disabled?: boolean
}) {
  return (
    <select
      className="w-full rounded-xl border border-slate-700 bg-slate-950/80 px-4 py-3 text-sm text-slate-100 outline-none transition focus:border-blue-400 disabled:opacity-60"
      value={value}
      disabled={disabled}
      onChange={(event) => onChange(event.target.value)}
    >
      {options.map((option) => (
        <option key={option.value || 'empty'} value={option.value}>
          {option.label}
        </option>
      ))}
    </select>
  )
}

function InlineError({ message }: { message: string }) {
  return (
    <div className="rounded-2xl border border-rose-500/30 bg-rose-500/10 px-4 py-3 text-sm text-rose-200">
      {message}
    </div>
  )
}

function AttachmentList({
  title,
  attachments,
}: {
  title: string
  attachments: AttachmentResponse[]
}) {
  if (attachments.length === 0) {
    return null
  }

  return (
    <div className="rounded-2xl border border-slate-800 bg-slate-900/60 p-4">
      <div className="text-sm font-medium text-white">{title}</div>
      <ul className="mt-3 space-y-2 text-sm text-slate-300">
        {attachments.map((attachment) => (
          <li key={attachment.attachmentId}>
            {attachment.fileName} · {formatFileSize(attachment.fileSize)}
          </li>
        ))}
      </ul>
    </div>
  )
}

function RecordList({
  emptyText,
  items,
}: {
  emptyText: string
  items: Array<{
    id: string
    title: string
    description: string
    meta: string[]
    extra?: string
    onEdit: () => void
    onDelete: () => void
    action?: ReactNode
  }>
}) {
  if (items.length === 0) {
    return <p className="text-sm text-slate-400">{emptyText}</p>
  }

  return (
    <div className="space-y-4">
      {items.map((item) => (
        <div
          key={item.id}
          className="rounded-2xl border border-slate-800 bg-slate-900/70 p-5"
        >
          <div className="flex flex-wrap items-start justify-between gap-4">
            <div>
              <h3 className="text-lg font-semibold text-white">{item.title}</h3>
              <p className="mt-2 text-sm text-slate-400">{item.description}</p>
            </div>
            <div className="flex flex-wrap gap-3">
              {item.action}
              <Button variant="secondary" onClick={item.onEdit}>
                Изменить
              </Button>
              <Button variant="secondary" onClick={item.onDelete}>
                Удалить
              </Button>
            </div>
          </div>
          <div className="mt-4 flex flex-wrap gap-2 text-xs text-slate-300">
            {item.meta.map((meta) => (
              <span
                key={`${item.id}-${meta}`}
                className="rounded-full border border-slate-700 px-3 py-1"
              >
                {meta}
              </span>
            ))}
          </div>
          {item.extra ? <p className="mt-3 text-sm text-slate-400">{item.extra}</p> : null}
        </div>
      ))}
    </div>
  )
}

function BlockEditor({
  block,
  onSave,
  onDelete,
}: {
  block: BlockResponse
  onSave: (payload: {
    blockId: string
    title: string
    position: number
    isVisible: boolean
  }) => void
  onDelete: (blockId: string) => void
}) {
  const [title, setTitle] = useState(block.title)
  const [position, setPosition] = useState(String(block.position))
  const [isVisible, setIsVisible] = useState(block.isVisible)

  return (
    <div className="rounded-2xl border border-slate-800 bg-slate-900/70 p-4">
      <div className="grid gap-3 md:grid-cols-[1fr_120px_auto_auto]">
        <Input value={title} onChange={(event) => setTitle(event.target.value)} />
        <Input
          type="number"
          value={position}
          onChange={(event) => setPosition(event.target.value)}
        />
        <label className="flex items-center gap-2 text-sm text-slate-200">
          <input
            type="checkbox"
            checked={isVisible}
            onChange={(event) => setIsVisible(event.target.checked)}
          />
          Видим
        </label>
        <div className="flex gap-2">
          <Button
            variant="secondary"
            onClick={() =>
              onSave({
                blockId: block.blockId,
                title: title.trim(),
                position: Number(position),
                isVisible,
              })
            }
          >
            Сохранить
          </Button>
          <Button variant="secondary" onClick={() => onDelete(block.blockId)}>
            Удалить
          </Button>
        </div>
      </div>
    </div>
  )
}
