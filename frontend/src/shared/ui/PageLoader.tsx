interface PageLoaderProps {
  label?: string
}

export function PageLoader({ label = 'Загружаем...' }: PageLoaderProps) {
  return (
    <div className="flex min-h-[40vh] items-center justify-center">
      <div className="rounded-2xl border border-slate-800 bg-slate-950/70 px-6 py-5 text-sm text-slate-300">
        {label}
      </div>
    </div>
  )
}
