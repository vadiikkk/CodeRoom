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
  EnrollmentResponse,
  GroupResponse,
  MaterialResponse,
  RoleInCourse,
} from '@/entities/course/types'
import { authApi } from '@/shared/api/auth'
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
  deadlineDate: string
  deadlineTime: string
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
  deadlineDate: '',
  deadlineTime: '23:59',
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
  const [editingMaterialId, setEditingMaterialId] = useState<string | null>(null)
  const [materialDraft, setMaterialDraft] = useState<MaterialDraft>(emptyMaterialDraft())
  const [editingAssignmentId, setEditingAssignmentId] = useState<string | null>(null)
  const [assignmentDraft, setAssignmentDraft] = useState<AssignmentDraft>(emptyAssignmentDraft())
  const [enrollEmail, setEnrollEmail] = useState('')
  const [enrollRole, setEnrollRole] = useState<RoleInCourse>('STUDENT')
  const [bulkEmails, setBulkEmails] = useState('')
  const [bulkRole, setBulkRole] = useState<RoleInCourse>('STUDENT')
  const [bulkResult, setBulkResult] = useState<string | null>(null)
  const [newGroupName, setNewGroupName] = useState('')
  const [addMemberEmail, setAddMemberEmail] = useState<Record<string, string>>({})
  const [lastError, setLastError] = useState<string | null>(null)

  const onMutationError = (error: unknown) => {
    setLastError(getErrorMessage(error))
  }

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

  const enrollmentsQuery = useQuery({
    queryKey: ['course-enrollments', courseId],
    queryFn: () => coursesApi.listEnrollments(courseId!),
    enabled: Boolean(courseId),
  })

  const enrollmentUserIds = useMemo(
    () => enrollmentsQuery.data?.map((e) => e.userId) ?? [],
    [enrollmentsQuery.data],
  )

  const enrollmentUsersQuery = useQuery({
    queryKey: ['users-lookup', enrollmentUserIds],
    queryFn: () => authApi.lookupByIds({ userIds: enrollmentUserIds }),
    enabled: enrollmentUserIds.length > 0,
  })

  const enrollmentEmailMap = useMemo(() => {
    const map = new Map<string, string>()
    for (const u of enrollmentUsersQuery.data?.users ?? []) {
      map.set(u.userId, u.email)
    }
    return map
  }, [enrollmentUsersQuery.data])

  const addEnrollmentMutation = useMutation({
    mutationFn: async () => {
      const lookup = await authApi.lookupByEmails({ emails: [enrollEmail.trim()] })
      const user = lookup.users[0]
      if (!user) {
        throw new Error(`Пользователь ${enrollEmail.trim()} не найден`)
      }
      await coursesApi.upsertEnrollment(courseId!, { userId: user.userId, roleInCourse: enrollRole })
    },
    onSuccess: () => {
      setEnrollEmail('')
      queryClient.invalidateQueries({ queryKey: ['course-enrollments', courseId] })
    },
  })

  const removeEnrollmentMutation = useMutation({
    mutationFn: (userId: string) => coursesApi.deleteEnrollment(courseId!, userId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['course-enrollments', courseId] })
    },
    onError: onMutationError,
  })

  const bulkEnrollMutation = useMutation({
    mutationFn: () => {
      const emails = bulkEmails
        .split(/[\n,;]+/)
        .map((e) => e.trim())
        .filter(Boolean)
      if (emails.length === 0) throw new Error('Введите хотя бы один email')
      return coursesApi.upsertEnrollmentsByEmail(courseId!, { emails, roleInCourse: bulkRole })
    },
    onSuccess: (result) => {
      setBulkEmails('')
      queryClient.invalidateQueries({ queryKey: ['course-enrollments', courseId] })
      setLastError(null)
      setBulkResult(`Добавлено/обновлено: ${result.addedOrUpdated}`)
    },
  })

  const groupsQuery = useQuery({
    queryKey: ['course-groups', courseId],
    queryFn: () => coursesApi.listGroups(courseId!),
    enabled: Boolean(courseId),
  })

  const createGroupMutation = useMutation({
    mutationFn: () => coursesApi.createGroup(courseId!, { name: newGroupName.trim() }),
    onSuccess: () => {
      setNewGroupName('')
      queryClient.invalidateQueries({ queryKey: ['course-groups', courseId] })
    },
  })

  const deleteGroupMutation = useMutation({
    mutationFn: (groupId: string) => coursesApi.deleteGroup(courseId!, groupId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['course-groups', courseId] }),
    onError: onMutationError,
  })

  const renameGroupMutation = useMutation({
    mutationFn: ({ groupId, name }: { groupId: string; name: string }) =>
      coursesApi.updateGroup(courseId!, groupId, { name }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['course-groups', courseId] }),
    onError: onMutationError,
  })

  const addGroupMemberMutation = useMutation({
    mutationFn: async ({ groupId, email }: { groupId: string; email: string }) => {
      const lookup = await authApi.lookupByEmails({ emails: [email.trim()] })
      const user = lookup.users[0]
      if (!user) throw new Error(`Пользователь ${email.trim()} не найден`)
      return coursesApi.addGroupMember(courseId!, groupId, { userId: user.userId })
    },
    onSuccess: (_data, variables) => {
      setAddMemberEmail((prev) => ({ ...prev, [variables.groupId]: '' }))
      queryClient.invalidateQueries({ queryKey: ['course-groups', courseId] })
    },
  })

  const removeGroupMemberMutation = useMutation({
    mutationFn: ({ groupId, userId }: { groupId: string; userId: string }) =>
      coursesApi.removeGroupMember(courseId!, groupId, userId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['course-groups', courseId] }),
    onError: onMutationError,
  })

  const deleteCourseMutation = useMutation({
    mutationFn: () => coursesApi.deleteCourse(courseId!),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['courses'] })
      window.location.href = '/app/courses'
    },
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

  const sortedBlocks = useMemo(
    () => blocks.slice().sort((a, b) => a.position - b.position),
    [blocks],
  )

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
    onError: onMutationError,
  })

  const createBlockMutation = useMutation({
    mutationFn: () => {
      const maxPos = blocks.length > 0 ? Math.max(...blocks.map((b) => b.position)) : 0
      return coursesApi.createBlock(courseId!, {
        title: newBlockTitle.trim(),
        position: maxPos + 10,
        isVisible: true,
      })
    },
    onSuccess: async () => {
      setNewBlockTitle('')
      await invalidateCourseData()
    },
    onError: onMutationError,
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
    onError: onMutationError,
  })

  const deleteBlockMutation = useMutation({
    mutationFn: (blockId: string) => coursesApi.deleteBlock(courseId!, blockId),
    onSuccess: invalidateCourseData,
    onError: onMutationError,
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

      const maxMaterialPos = materials.length > 0 ? Math.max(...materials.map((m) => m.position)) : 0

      const payload: CreateMaterialRequest = {
        title: materialDraft.title.trim(),
        description: materialDraft.description.trim() || undefined,
        body: materialDraft.body.trim() || undefined,
        blockId: materialDraft.blockId || undefined,
        position: maxMaterialPos + 10,
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
    onError: onMutationError,
  })

  const saveAssignmentMutation = useMutation({
    mutationFn: async () => {
      const attachmentIds = [
        ...assignmentDraft.existingAttachments.map((attachment) => attachment.attachmentId),
        ...(await uploadFiles(courseId!, assignmentDraft.files)),
      ]

      const parsedDeadline = parseDeadlineDraft(
        assignmentDraft.deadlineDate,
        assignmentDraft.deadlineTime,
      )
      if (parsedDeadline.error) {
        throw new Error(parsedDeadline.error)
      }
      const deadlineAt = parsedDeadline.iso

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

      const maxAssignmentPos = assignments.length > 0 ? Math.max(...assignments.map((a) => a.position)) : 0

      const payload: CreateAssignmentRequest = {
        title: assignmentDraft.title.trim(),
        description: assignmentDraft.description.trim() || undefined,
        assignmentType: assignmentDraft.assignmentType,
        workType: assignmentDraft.workType,
        deadlineAt,
        weight: Number(assignmentDraft.weight),
        blockId: assignmentDraft.blockId || undefined,
        position: maxAssignmentPos + 10,
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
    onError: onMutationError,
  })

  const deleteAssignmentMutation = useMutation({
    mutationFn: (assignmentId: string) => coursesApi.deleteAssignment(assignmentId),
    onSuccess: invalidateCourseData,
    onError: onMutationError,
  })

  const publishAssignmentMutation = useMutation({
    mutationFn: (assignmentId: string) => coursesApi.publishAssignment(assignmentId),
    onSuccess: invalidateCourseData,
    onError: onMutationError,
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
    githubPatQuery.isPending ||
    enrollmentsQuery.isPending ||
    groupsQuery.isPending
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
    enrollmentsQuery.error,
    groupsQuery.error,
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
  const enrollments = enrollmentsQuery.data!
  const groups = groupsQuery.data!

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
      {lastError ? (
        <div className="flex items-start justify-between rounded-2xl border border-rose-500/30 bg-rose-500/10 px-4 py-3 text-sm text-rose-200">
          <span>{lastError}</span>
          <button
            className="ml-4 shrink-0 text-rose-400 transition hover:text-rose-200"
            onClick={() => setLastError(null)}
          >
            ✕
          </button>
        </div>
      ) : null}

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

      <section className="flex flex-wrap gap-3">
        <Link
          to={`/app/courses/${courseId}/gradebook`}
          className="inline-flex rounded-xl border border-slate-700 bg-slate-900 px-4 py-2.5 text-sm font-medium text-slate-100 transition hover:border-slate-600 hover:bg-slate-800"
        >
          Ведомость
        </Link>
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
              Токен нужен для интеграции с GitHub-репозиториями.
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
            <div className="grid gap-3 md:grid-cols-[1fr_auto]">
              <Input
                value={newBlockTitle}
                onChange={(event) => setNewBlockTitle(event.target.value)}
                placeholder="Название блока"
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
              <div className="space-y-3">
                {sortedBlocks.map((block, index) => (
                  <BlockEditor
                    key={block.blockId}
                    block={block}
                    isFirst={index === 0}
                    isLast={index === sortedBlocks.length - 1}
                    onSave={(payload) => updateBlockMutation.mutate(payload)}
                    onDelete={(blockId) => deleteBlockMutation.mutate(blockId)}
                    onMoveUp={() => {
                      const prev = sortedBlocks[index - 1]
                      updateBlockMutation.mutate({
                        blockId: block.blockId,
                        title: block.title,
                        position: prev.position,
                        isVisible: block.isVisible,
                      })
                      updateBlockMutation.mutate({
                        blockId: prev.blockId,
                        title: prev.title,
                        position: block.position,
                        isVisible: prev.isVisible,
                      })
                    }}
                    onMoveDown={() => {
                      const next = sortedBlocks[index + 1]
                      updateBlockMutation.mutate({
                        blockId: block.blockId,
                        title: block.title,
                        position: next.position,
                        isVisible: block.isVisible,
                      })
                      updateBlockMutation.mutate({
                        blockId: next.blockId,
                        title: next.title,
                        position: block.position,
                        isVisible: next.isVisible,
                      })
                    }}
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
            <Field label="Блок">
              <SelectField
                value={materialDraft.blockId}
                onChange={(value) =>
                  setMaterialDraft((prev) => ({ ...prev, blockId: value }))
                }
                options={buildBlockOptions(blocks)}
              />
            </Field>
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
              <Field label="Дедлайн">
                <div className="flex flex-col gap-2 sm:flex-row sm:flex-wrap sm:items-end sm:gap-3">
                  <Input
                    type="date"
                    className="date-time-picker-input w-full shrink-0 sm:min-w-[15rem] sm:max-w-none"
                    value={assignmentDraft.deadlineDate}
                    min="2000-01-01"
                    max="2100-12-31"
                    onChange={(event) =>
                      setAssignmentDraft((prev) => ({ ...prev, deadlineDate: event.target.value }))
                    }
                  />
                  <Input
                    type="time"
                    className="date-time-picker-input w-full shrink-0 sm:min-w-[8.5rem] sm:max-w-none"
                    value={assignmentDraft.deadlineTime}
                    onChange={(event) =>
                      setAssignmentDraft((prev) => ({ ...prev, deadlineTime: event.target.value }))
                    }
                  />
                  <button
                    type="button"
                    className="shrink-0 rounded-xl border border-slate-600 px-3 py-2 text-sm text-slate-300 transition hover:border-slate-500 hover:text-white"
                    onClick={() =>
                      setAssignmentDraft((prev) => ({ ...prev, deadlineDate: '', deadlineTime: '' }))
                    }
                  >
                    Без срока
                  </button>
                </div>
                <p className="mt-2 text-xs text-slate-500">
                  Укажите дату и время или нажмите «Без срока», если дедлайн не нужен.
                </p>
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
                action: (
                  <>
                    <Link
                      to={`/app/courses/${courseId}/assignments/${assignment.assignmentId}/grading`}
                      className="inline-flex items-center justify-center rounded-xl border border-slate-700 bg-slate-900 px-4 py-2.5 text-sm font-medium text-slate-100 transition hover:border-slate-600 hover:bg-slate-800"
                    >
                      Проверить
                    </Link>
                    {assignment.assignmentType === 'CODE' && !assignment.code?.publishedAt ? (
                      <Button
                        variant="secondary"
                        onClick={() => publishAssignmentMutation.mutate(assignment.assignmentId)}
                        isLoading={publishAssignmentMutation.isPending}
                      >
                        Опубликовать
                      </Button>
                    ) : null}
                  </>
                ),
              }))}
          />
        </PanelCard>
      </section>

      <PanelCard title="Участники курса">
        <div className="space-y-4">
          <div className="grid gap-3 md:grid-cols-[1fr_160px_auto]">
            <Input
              value={enrollEmail}
              onChange={(event) => setEnrollEmail(event.target.value)}
              placeholder="Email пользователя"
            />
            <select
              className="w-full rounded-xl border border-slate-700 bg-slate-950/80 px-4 py-3 text-sm text-slate-100 outline-none transition focus:border-blue-400"
              value={enrollRole}
              onChange={(event) => setEnrollRole(event.target.value as RoleInCourse)}
            >
              <option value="STUDENT">Студент</option>
              <option value="ASSISTANT">Ассистент</option>
              <option value="TEACHER">Преподаватель</option>
            </select>
            <Button
              onClick={() => addEnrollmentMutation.mutate()}
              isLoading={addEnrollmentMutation.isPending}
            >
              Добавить
            </Button>
          </div>
          {addEnrollmentMutation.isError ? (
            <InlineError message={getErrorMessage(addEnrollmentMutation.error)} />
          ) : null}

          {enrollments.length === 0 ? (
            <p className="text-sm text-slate-500">Участников пока нет.</p>
          ) : (
            <div className="space-y-2">
              {enrollments.map((enrollment) => (
                <EnrollmentRow
                  key={enrollment.userId}
                  enrollment={enrollment}
                  email={enrollmentEmailMap.get(enrollment.userId) ?? enrollment.userId.slice(0, 8)}
                  onRemove={() => removeEnrollmentMutation.mutate(enrollment.userId)}
                  isRemoving={removeEnrollmentMutation.isPending}
                />
              ))}
            </div>
          )}
          <div className="border-t border-slate-800 pt-4">
            <h3 className="text-sm font-semibold text-white">Добавление нескольких пользователей</h3>
            <p className="mt-1 text-xs text-slate-500">
              Введите email через запятую, точку с запятой или с новой строки.
            </p>
            <Textarea
              className="mt-3"
              rows={4}
              value={bulkEmails}
              onChange={(event) => setBulkEmails(event.target.value)}
              placeholder={'student1@example.com\nstudent2@example.com'}
            />
            <div className="mt-3 flex flex-wrap items-end gap-3">
              <select
                className="rounded-xl border border-slate-700 bg-slate-950/80 px-4 py-3 text-sm text-slate-100 outline-none transition focus:border-blue-400"
                value={bulkRole}
                onChange={(event) => setBulkRole(event.target.value as RoleInCourse)}
              >
                <option value="STUDENT">Студент</option>
                <option value="ASSISTANT">Ассистент</option>
                <option value="TEACHER">Преподаватель</option>
              </select>
              <Button
                onClick={() => bulkEnrollMutation.mutate()}
                isLoading={bulkEnrollMutation.isPending}
              >
                Добавить списком
              </Button>
            </div>
            {bulkEnrollMutation.isError ? (
              <InlineError message={getErrorMessage(bulkEnrollMutation.error)} />
            ) : null}
            {bulkResult ? (
              <div className="mt-2 rounded-2xl border border-green-500/30 bg-green-500/10 px-4 py-3 text-sm text-green-200">
                {bulkResult}
              </div>
            ) : null}
          </div>
        </div>
      </PanelCard>

      <PanelCard title="Группы">
        <div className="space-y-4">
          <div className="grid gap-3 md:grid-cols-[1fr_auto]">
            <Input
              value={newGroupName}
              onChange={(event) => setNewGroupName(event.target.value)}
              placeholder="Название группы"
            />
            <Button
              onClick={() => createGroupMutation.mutate()}
              isLoading={createGroupMutation.isPending}
            >
              Создать группу
            </Button>
          </div>
          {createGroupMutation.isError ? (
            <InlineError message={getErrorMessage(createGroupMutation.error)} />
          ) : null}

          {groups.length === 0 ? (
            <p className="text-sm text-slate-500">Групп пока нет.</p>
          ) : (
            <div className="space-y-4">
              {groups.map((group) => (
                <GroupCard
                  key={group.groupId}
                  group={group}
                  emailMap={enrollmentEmailMap}
                  memberEmail={addMemberEmail[group.groupId] ?? ''}
                  onMemberEmailChange={(value) =>
                    setAddMemberEmail((prev) => ({ ...prev, [group.groupId]: value }))
                  }
                  onAddMember={() =>
                    addGroupMemberMutation.mutate({
                      groupId: group.groupId,
                      email: addMemberEmail[group.groupId] ?? '',
                    })
                  }
                  onRemoveMember={(userId) =>
                    removeGroupMemberMutation.mutate({ groupId: group.groupId, userId })
                  }
                  onRename={(name) =>
                    renameGroupMutation.mutate({ groupId: group.groupId, name })
                  }
                  onDelete={() => deleteGroupMutation.mutate(group.groupId)}
                  isAddingMember={addGroupMemberMutation.isPending}
                  addMemberError={addGroupMemberMutation.isError ? getErrorMessage(addGroupMemberMutation.error) : null}
                />
              ))}
            </div>
          )}
        </div>
      </PanelCard>

      <PanelCard title="Опасная зона">
        <div className="space-y-3">
          <p className="text-sm text-slate-400">
            Удаление курса необратимо. Все материалы, задания, сдачи и оценки будут удалены.
          </p>
          {deleteCourseMutation.isError ? (
            <InlineError message={getErrorMessage(deleteCourseMutation.error)} />
          ) : null}
          <Button
            variant="secondary"
            className="border-rose-500/40 text-rose-300 hover:border-rose-400 hover:bg-rose-500/10"
            onClick={() => {
              if (window.confirm(`Вы уверены, что хотите удалить курс "${course.title}"?`)) {
                deleteCourseMutation.mutate()
              }
            }}
            isLoading={deleteCourseMutation.isPending}
          >
            Удалить курс
          </Button>
        </div>
      </PanelCard>
    </div>
  )
}

function GroupCard({
  group,
  emailMap,
  memberEmail,
  onMemberEmailChange,
  onAddMember,
  onRemoveMember,
  onRename,
  onDelete,
  isAddingMember,
  addMemberError,
}: {
  group: GroupResponse
  emailMap: Map<string, string>
  memberEmail: string
  onMemberEmailChange: (value: string) => void
  onAddMember: () => void
  onRemoveMember: (userId: string) => void
  onRename: (name: string) => void
  onDelete: () => void
  isAddingMember: boolean
  addMemberError: string | null
}) {
  const [isEditing, setIsEditing] = useState(false)
  const [editName, setEditName] = useState(group.name)

  return (
    <div className="rounded-2xl border border-slate-800 bg-slate-900/70 p-5">
      <div className="flex flex-wrap items-start justify-between gap-3">
        {isEditing ? (
          <div className="flex flex-1 items-center gap-2">
            <Input
              value={editName}
              onChange={(event) => setEditName(event.target.value)}
            />
            <Button
              onClick={() => {
                onRename(editName.trim())
                setIsEditing(false)
              }}
            >
              Ок
            </Button>
            <Button variant="ghost" onClick={() => setIsEditing(false)}>
              Отмена
            </Button>
          </div>
        ) : (
          <div>
            <div className="text-sm font-semibold text-white">{group.name}</div>
            <div className="mt-0.5 text-xs text-slate-500">
              {group.members.length} участников · {formatDateTime(group.createdAt)}
            </div>
          </div>
        )}
        {!isEditing ? (
          <div className="flex gap-2">
            <Button variant="ghost" onClick={() => { setEditName(group.name); setIsEditing(true) }}>
              Переименовать
            </Button>
            <Button variant="ghost" onClick={onDelete}>
              Удалить
            </Button>
          </div>
        ) : null}
      </div>

      {group.members.length > 0 ? (
        <div className="mt-4 space-y-1">
          {group.members.map((member) => (
            <div
              key={member.userId}
              className="flex items-center justify-between rounded-xl px-3 py-2 text-sm hover:bg-slate-800/60"
            >
              <span className="text-slate-300">
                {emailMap.get(member.userId) ?? member.userId.slice(0, 8)}
              </span>
              <button
                className="text-xs text-slate-500 transition hover:text-rose-400"
                onClick={() => onRemoveMember(member.userId)}
              >
                Убрать
              </button>
            </div>
          ))}
        </div>
      ) : null}

      <div className="mt-3 grid gap-2 md:grid-cols-[1fr_auto]">
        <Input
          value={memberEmail}
          onChange={(event) => onMemberEmailChange(event.target.value)}
          placeholder="Email участника"
        />
        <Button variant="secondary" onClick={onAddMember} isLoading={isAddingMember}>
          Добавить
        </Button>
      </div>
      {addMemberError ? (
        <div className="mt-2 text-xs text-rose-400">{addMemberError}</div>
      ) : null}
    </div>
  )
}

function EnrollmentRow({
  enrollment,
  email,
  onRemove,
  isRemoving,
}: {
  enrollment: EnrollmentResponse
  email: string
  onRemove: () => void
  isRemoving: boolean
}) {
  const roleLabels: Record<RoleInCourse, string> = {
    TEACHER: 'Преподаватель',
    ASSISTANT: 'Ассистент',
    STUDENT: 'Студент',
  }

  return (
    <div className="flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-slate-800 bg-slate-900/70 p-4">
      <div>
        <div className="text-sm font-medium text-white">{email}</div>
        <div className="mt-0.5 text-xs text-slate-500">
          {roleLabels[enrollment.roleInCourse]} · {formatDateTime(enrollment.createdAt)}
        </div>
      </div>
      <Button variant="ghost" onClick={onRemove} isLoading={isRemoving}>
        Удалить
      </Button>
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
        label: block.title,
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
    ...splitDeadlineFromIso(assignment.deadlineAt),
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

function splitDeadlineFromIso(iso: string | null | undefined): Pick<AssignmentDraft, 'deadlineDate' | 'deadlineTime'> {
  if (!iso) {
    return { deadlineDate: '', deadlineTime: '' }
  }
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) {
    return { deadlineDate: '', deadlineTime: '' }
  }
  const y = d.getFullYear()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  const h = String(d.getHours()).padStart(2, '0')
  const min = String(d.getMinutes()).padStart(2, '0')
  return { deadlineDate: `${y}-${m}-${day}`, deadlineTime: `${h}:${min}` }
}

function parseDeadlineDraft(
  deadlineDate: string,
  deadlineTime: string,
): { iso: string | undefined; error?: string } {
  const date = deadlineDate.trim()
  const time = deadlineTime.trim()
  if (!date && !time) {
    return { iso: undefined }
  }
  if (!date || !time) {
    return {
      iso: undefined,
      error: 'Укажите и дату, и время дедлайна или снимите оба поля.',
    }
  }
  const local = new Date(`${date}T${time}`)
  if (Number.isNaN(local.getTime())) {
    return { iso: undefined, error: 'Некорректная дата или время.' }
  }
  const year = local.getFullYear()
  if (year < 2000 || year > 2100) {
    return { iso: undefined, error: 'Год должен быть в диапазоне 2000–2100.' }
  }
  return { iso: local.toISOString() }
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
  isFirst,
  isLast,
  onSave,
  onDelete,
  onMoveUp,
  onMoveDown,
}: {
  block: BlockResponse
  isFirst: boolean
  isLast: boolean
  onSave: (payload: {
    blockId: string
    title: string
    position: number
    isVisible: boolean
  }) => void
  onDelete: (blockId: string) => void
  onMoveUp: () => void
  onMoveDown: () => void
}) {
  const [isEditing, setIsEditing] = useState(false)
  const [title, setTitle] = useState(block.title)
  const [isVisible, setIsVisible] = useState(block.isVisible)

  return (
    <div className="rounded-2xl border border-slate-800 bg-slate-900/70 p-4">
      <div className="flex items-center gap-3">
        <div className="flex flex-col gap-0.5">
          <button
            className="rounded px-1 text-slate-500 transition hover:bg-slate-800 hover:text-slate-200 disabled:opacity-30 disabled:hover:bg-transparent disabled:hover:text-slate-500"
            disabled={isFirst}
            onClick={onMoveUp}
            title="Переместить вверх"
          >
            ▲
          </button>
          <button
            className="rounded px-1 text-slate-500 transition hover:bg-slate-800 hover:text-slate-200 disabled:opacity-30 disabled:hover:bg-transparent disabled:hover:text-slate-500"
            disabled={isLast}
            onClick={onMoveDown}
            title="Переместить вниз"
          >
            ▼
          </button>
        </div>

        <div className="min-w-0 flex-1">
          {isEditing ? (
            <Input value={title} onChange={(event) => setTitle(event.target.value)} />
          ) : (
            <div className="text-sm font-medium text-white">{block.title}</div>
          )}
        </div>

        <label className="flex shrink-0 items-center gap-1.5 text-xs text-slate-400">
          <input
            type="checkbox"
            checked={isEditing ? isVisible : block.isVisible}
            onChange={(event) => {
              if (isEditing) {
                setIsVisible(event.target.checked)
              } else {
                onSave({
                  blockId: block.blockId,
                  title: block.title,
                  position: block.position,
                  isVisible: event.target.checked,
                })
              }
            }}
          />
          {(isEditing ? isVisible : block.isVisible) ? 'Видим' : 'Скрыт'}
        </label>

        <div className="flex shrink-0 gap-1.5">
          {isEditing ? (
            <>
              <Button
                variant="secondary"
                onClick={() => {
                  onSave({
                    blockId: block.blockId,
                    title: title.trim(),
                    position: block.position,
                    isVisible,
                  })
                  setIsEditing(false)
                }}
              >
                Сохранить
              </Button>
              <Button
                variant="ghost"
                onClick={() => {
                  setTitle(block.title)
                  setIsVisible(block.isVisible)
                  setIsEditing(false)
                }}
              >
                Отмена
              </Button>
            </>
          ) : (
            <>
              <Button variant="ghost" onClick={() => setIsEditing(true)}>
                Изменить
              </Button>
              <Button variant="ghost" onClick={() => onDelete(block.blockId)}>
                Удалить
              </Button>
            </>
          )}
        </div>
      </div>
    </div>
  )
}
