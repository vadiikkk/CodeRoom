import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { Link } from 'react-router-dom'
import { z } from 'zod'

import { useAuthStore } from '@/features/auth/model/auth-store'
import { coursesApi } from '@/shared/api/courses'
import { getErrorMessage } from '@/shared/lib/errors'
import { formatDateTime } from '@/shared/lib/format'
import { Button } from '@/shared/ui/Button'
import { EmptyState } from '@/shared/ui/EmptyState'
import { FieldError } from '@/shared/ui/FieldError'
import { Input, Textarea } from '@/shared/ui/Input'
import { PageLoader } from '@/shared/ui/PageLoader'

const createCourseSchema = z.object({
  title: z.string().min(1, 'Введите название курса').max(200, 'Слишком длинное название'),
  description: z.string().max(10000, 'Слишком длинное описание').optional(),
})

type CreateCourseFormValues = z.infer<typeof createCourseSchema>

export function CoursesPage() {
  const queryClient = useQueryClient()
  const { currentUser } = useAuthStore()
  const canCreateCourse = currentUser?.role === 'TEACHER'

  const coursesQuery = useQuery({
    queryKey: ['courses'],
    queryFn: coursesApi.listCourses,
  })

  const form = useForm<CreateCourseFormValues>({
    resolver: zodResolver(createCourseSchema),
    defaultValues: {
      title: '',
      description: '',
    },
  })

  const createCourseMutation = useMutation({
    mutationFn: (values: CreateCourseFormValues) =>
      coursesApi.createCourse({
        title: values.title,
        description: values.description,
        isVisible: false,
      }),
    onSuccess: () => {
      form.reset()
      queryClient.invalidateQueries({ queryKey: ['courses'] })
    },
  })

  if (coursesQuery.isPending) {
    return <PageLoader label="Загружаем курсы..." />
  }

  if (coursesQuery.isError) {
    return (
      <EmptyState
        title="Не удалось загрузить курсы"
        description={getErrorMessage(coursesQuery.error)}
      />
    )
  }

  return (
    <div className="space-y-8">
      <section className="rounded-3xl border border-slate-800 bg-slate-950/70 p-8">
        <div className="flex flex-col gap-3 lg:flex-row lg:items-end lg:justify-between">
          <div>
            <p className="text-sm uppercase tracking-[0.28em] text-blue-300">
              Dashboard
            </p>
            <h1 className="mt-3 text-3xl font-semibold text-white">Мои курсы</h1>
            <p className="mt-3 max-w-2xl text-sm text-slate-400">
              Здесь собраны все курсы, в которых вы участвуете. Откройте нужный
              курс, чтобы перейти к материалам, заданиям и результатам.
            </p>
          </div>
          <div className="rounded-2xl border border-slate-800 bg-slate-900/80 px-4 py-3 text-sm text-slate-300">
            {currentUser?.email}
          </div>
        </div>
      </section>

      {canCreateCourse ? (
        <section className="grid gap-6 lg:grid-cols-[1.2fr_0.8fr]">
          <div className="rounded-3xl border border-slate-800 bg-slate-950/70 p-6">
            <div className="mb-5">
              <h2 className="text-xl font-semibold text-white">Создать курс</h2>
              <p className="mt-2 text-sm text-slate-400">
                Преподаватель может создать новый курс и сразу увидеть его в списке.
              </p>
            </div>

            <form
              className="space-y-4"
              onSubmit={form.handleSubmit((values) => createCourseMutation.mutate(values))}
            >
              <label className="block space-y-2">
                <span className="text-sm text-slate-300">Название</span>
                <Input placeholder="Алгоритмы и структуры данных" {...form.register('title')} />
                <FieldError message={form.formState.errors.title?.message} />
              </label>

              <label className="block space-y-2">
                <span className="text-sm text-slate-300">Описание</span>
                <Textarea
                  rows={5}
                  placeholder="Курс по базовым алгоритмам, структурам данных и анализу сложности."
                  {...form.register('description')}
                />
                <FieldError message={form.formState.errors.description?.message} />
              </label>

              {createCourseMutation.isError ? (
                <div className="rounded-2xl border border-rose-500/30 bg-rose-500/10 px-4 py-3 text-sm text-rose-200">
                  {getErrorMessage(createCourseMutation.error, 'Не удалось создать курс')}
                </div>
              ) : null}

              <Button type="submit" isLoading={createCourseMutation.isPending}>
                Создать черновик курса
              </Button>
            </form>
          </div>

          <div className="rounded-3xl border border-slate-800 bg-slate-950/70 p-6">
            <h2 className="text-xl font-semibold text-white">Возможности</h2>
            <ul className="mt-4 space-y-3 text-sm text-slate-300">
              <li>Быстрый доступ к вашим курсам</li>
              <li>Переход к структуре, материалам и заданиям</li>
              <li>Разделение сценариев для преподавателя и студента</li>
              <li>Создание нового курса из личного кабинета</li>
            </ul>
          </div>
        </section>
      ) : null}

      <section className="grid gap-5 md:grid-cols-2 xl:grid-cols-3">
        {coursesQuery.data.length === 0 ? (
          <div className="md:col-span-2 xl:col-span-3">
            <EmptyState
              title="Курсов пока нет"
              description={
                canCreateCourse
                  ? 'Создайте первый курс через форму выше.'
                  : 'Когда преподаватель добавит вас в курс, он появится здесь.'
              }
            />
          </div>
        ) : (
          coursesQuery.data.map((course) => (
            <Link
              key={course.courseId}
              to={`/app/courses/${course.courseId}`}
              className="rounded-3xl border border-slate-800 bg-slate-950/70 p-6 transition hover:border-blue-500/60 hover:bg-slate-900/90"
            >
              <div className="flex items-start justify-between gap-4">
                <div>
                  <h3 className="text-xl font-semibold text-white">{course.title}</h3>
                  <p className="mt-2 text-sm text-slate-400">
                    {course.description || 'Описание пока не добавлено.'}
                  </p>
                </div>
                <span className="rounded-full border border-slate-700 px-3 py-1 text-xs font-medium text-slate-300">
                  {course.myRoleInCourse}
                </span>
              </div>

              <div className="mt-6 flex flex-wrap gap-2 text-xs text-slate-400">
                <span className="rounded-full bg-slate-900 px-3 py-1">
                  {course.isVisible ? 'Виден студентам' : 'Черновик'}
                </span>
                <span className="rounded-full bg-slate-900 px-3 py-1">
                  GitHub PAT: {course.githubPatConfigured ? 'настроен' : 'не настроен'}
                </span>
              </div>

              <p className="mt-6 text-xs text-slate-500">
                Обновлен: {formatDateTime(course.updatedAt)}
              </p>
            </Link>
          ))
        )}
      </section>
    </div>
  )
}
