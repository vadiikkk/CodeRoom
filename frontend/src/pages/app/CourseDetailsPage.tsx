import { useQuery } from '@tanstack/react-query'
import { Link, useParams } from 'react-router-dom'

import type {
  AssignmentResponse,
  ItemResponse,
  MaterialResponse,
} from '@/entities/course/types'
import { coursesApi } from '@/shared/api/courses'
import { getErrorMessage } from '@/shared/lib/errors'
import { formatDateTime } from '@/shared/lib/format'
import { EmptyState } from '@/shared/ui/EmptyState'
import { PageLoader } from '@/shared/ui/PageLoader'

function getItemSummary(
  item: ItemResponse,
  materialsById: Map<string, MaterialResponse>,
  assignmentsById: Map<string, AssignmentResponse>,
) {
  if (item.itemType === 'MATERIAL') {
    const material = materialsById.get(item.refId)

    return {
      title: material?.title ?? 'Материал',
      description: material?.description ?? material?.body ?? 'Учебный материал курса',
      href: `/app/courses/${item.courseId}/materials/${item.refId}`,
    }
  }

  const assignment = assignmentsById.get(item.refId)

  return {
    title: assignment?.title ?? 'Задание',
    description:
      assignment?.description ??
      `Тип: ${assignment?.assignmentType ?? 'UNKNOWN'}, формат: ${assignment?.workType ?? 'UNKNOWN'}`,
    href: `/app/courses/${item.courseId}/assignments/${item.refId}`,
  }
}

export function CourseDetailsPage() {
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

  if (!courseId) {
    return (
      <EmptyState
        title="Курс не найден"
        description="В адресе отсутствует идентификатор курса."
      />
    )
  }

  const isPending =
    courseQuery.isPending ||
    membershipQuery.isPending ||
    structureQuery.isPending ||
    materialsQuery.isPending ||
    assignmentsQuery.isPending

  if (isPending) {
    return <PageLoader label="Собираем данные курса..." />
  }

  const firstError = [
    courseQuery.error,
    membershipQuery.error,
    structureQuery.error,
    materialsQuery.error,
    assignmentsQuery.error,
  ].find(Boolean)

  if (firstError) {
    return (
      <EmptyState
        title="Не удалось открыть курс"
        description={getErrorMessage(firstError)}
      />
    )
  }

  const course = courseQuery.data!
  const membership = membershipQuery.data!
  const structure = structureQuery.data!
  const materials = materialsQuery.data!
  const assignments = assignmentsQuery.data!
  const materialsById = new Map(materials.map((material) => [material.materialId, material]))
  const assignmentsById = new Map(
    assignments.map((assignment) => [assignment.assignmentId, assignment]),
  )

  return (
    <div className="space-y-8">
      <section className="rounded-3xl border border-slate-800 bg-slate-950/70 p-8">
        <div className="flex flex-col gap-5 lg:flex-row lg:items-start lg:justify-between">
          <div>
            <Link className="text-sm text-blue-300 hover:text-blue-200" to="/app/courses">
              ← Назад к курсам
            </Link>
            <h1 className="mt-4 text-3xl font-semibold text-white">{course.title}</h1>
            <p className="mt-3 max-w-3xl text-sm text-slate-400">
              {course.description || 'Описание курса пока не заполнено.'}
            </p>
          </div>

          <div className="flex flex-wrap gap-2 text-xs">
            <span className="rounded-full border border-slate-700 px-3 py-1 text-slate-300">
              Моя роль: {membership.roleInCourse}
            </span>
            <span className="rounded-full border border-slate-700 px-3 py-1 text-slate-300">
              {course.isVisible ? 'Курс опубликован' : 'Курс скрыт'}
            </span>
            <span className="rounded-full border border-slate-700 px-3 py-1 text-slate-300">
              PAT: {course.githubPatConfigured ? 'настроен' : 'не настроен'}
            </span>
          </div>
        </div>

        <div className="mt-8 grid gap-4 md:grid-cols-4">
          <StatCard label="Материалы" value={String(materials.length)} />
          <StatCard label="Задания" value={String(assignments.length)} />
          <StatCard label="Блоки" value={String(structure.blocks.length)} />
          <StatCard label="Обновлен" value={formatDateTime(course.updatedAt)} />
        </div>
      </section>

      <section className="grid gap-6 xl:grid-cols-[1.25fr_0.75fr]">
        <div className="space-y-6">
          {structure.rootItems.length > 0 ? (
            <ContentSection
              title="Корневые элементы"
              items={structure.rootItems}
              materialsById={materialsById}
              assignmentsById={assignmentsById}
            />
          ) : null}

          {structure.blocks.length === 0 && structure.rootItems.length === 0 ? (
            <EmptyState
              title="Структура курса пока пуста"
              description="Когда преподаватель добавит блоки и элементы, они появятся здесь."
            />
          ) : null}

          {structure.blocks.map((blockWithItems) => (
            <ContentSection
              key={blockWithItems.block.blockId}
              title={blockWithItems.block.title}
              subtitle={blockWithItems.block.isVisible ? 'Виден студентам' : 'Скрыт'}
              items={blockWithItems.items}
              materialsById={materialsById}
              assignmentsById={assignmentsById}
            />
          ))}
        </div>

        <aside className="space-y-6">
          <div className="rounded-3xl border border-slate-800 bg-slate-950/70 p-6">
            <h2 className="text-xl font-semibold text-white">Быстрые переходы</h2>
            <div className="mt-4 space-y-3">
              <Link
                to="/app/courses"
                className="inline-flex w-full items-center justify-center rounded-xl border border-slate-700 bg-slate-900 px-4 py-2.5 text-sm font-medium text-slate-100 transition hover:border-slate-600 hover:bg-slate-800"
              >
                К списку курсов
              </Link>
              {membership.roleInCourse === 'TEACHER' ? (
                <Link
                  to={`/app/courses/${courseId}/manage`}
                  className="inline-flex w-full items-center justify-center rounded-xl border border-blue-500/40 bg-blue-500/10 px-4 py-2.5 text-sm font-medium text-blue-100 transition hover:bg-blue-500/20"
                >
                  Управление курсом
                </Link>
              ) : null}
            </div>
            <p className="mt-4 text-sm text-slate-400">
              Выберите раздел курса слева, чтобы открыть материал или перейти к
              нужному заданию.
            </p>
          </div>

          <div className="rounded-3xl border border-slate-800 bg-slate-950/70 p-6">
            <h2 className="text-xl font-semibold text-white">О курсе</h2>
            <ul className="mt-4 space-y-3 text-sm text-slate-300">
              <li>Материалы и задания упорядочены по блокам</li>
              <li>Видимость элементов зависит от настроек курса</li>
              <li>Для каждого задания доступна отдельная страница с деталями</li>
            </ul>
          </div>
        </aside>
      </section>
    </div>
  )
}

interface StatCardProps {
  label: string
  value: string
}

function StatCard({ label, value }: StatCardProps) {
  return (
    <div className="rounded-2xl border border-slate-800 bg-slate-900/70 p-4">
      <div className="text-xs uppercase tracking-wide text-slate-500">{label}</div>
      <div className="mt-2 text-lg font-semibold text-white">{value}</div>
    </div>
  )
}

interface ContentSectionProps {
  title: string
  subtitle?: string
  items: ItemResponse[]
  materialsById: Map<string, MaterialResponse>
  assignmentsById: Map<string, AssignmentResponse>
}

function ContentSection({
  title,
  subtitle,
  items,
  materialsById,
  assignmentsById,
}: ContentSectionProps) {
  const sortedItems = [...items].sort((left, right) => left.position - right.position)

  return (
    <section className="rounded-3xl border border-slate-800 bg-slate-950/70 p-6">
      <div className="mb-5 flex items-center justify-between gap-4">
        <div>
          <h2 className="text-xl font-semibold text-white">{title}</h2>
          {subtitle ? <p className="mt-1 text-sm text-slate-400">{subtitle}</p> : null}
        </div>
      </div>

      <div className="space-y-4">
        {sortedItems.map((item) => {
          const summary = getItemSummary(item, materialsById, assignmentsById)

          return (
            <Link
              key={item.itemId}
              to={summary.href}
              className="block rounded-2xl border border-slate-800 bg-slate-900/70 p-5 transition hover:border-blue-500/60 hover:bg-slate-900"
            >
              <div className="flex flex-wrap items-center gap-3">
                <span className="rounded-full border border-slate-700 px-3 py-1 text-xs font-medium text-slate-300">
                  {item.itemType}
                </span>
                <span className="rounded-full border border-slate-700 px-3 py-1 text-xs font-medium text-slate-300">
                  Позиция {item.position}
                </span>
                {!item.isVisible ? (
                  <span className="rounded-full border border-amber-500/40 px-3 py-1 text-xs font-medium text-amber-200">
                    Скрыт
                  </span>
                ) : null}
              </div>
              <h3 className="mt-4 text-lg font-semibold text-white">{summary.title}</h3>
              <p className="mt-2 text-sm text-slate-400 line-clamp-3">{summary.description}</p>
            </Link>
          )
        })}
      </div>
    </section>
  )
}
