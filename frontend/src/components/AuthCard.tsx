import type { FormEvent, ReactNode } from 'react'

type AuthCardProps = {
  title: string
  subtitle: string
  error: string | null
  submitting: boolean
  submitLabel: string
  onSubmit: () => void
  footer: ReactNode
  children: ReactNode
}

/** Shared layout for the login and sign-up forms. */
export function AuthCard({ title, subtitle, error, submitting, submitLabel, onSubmit, footer, children }: AuthCardProps) {
  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    onSubmit()
  }

  return (
    <main className="min-h-screen bg-slate-950 text-slate-100 flex items-center justify-center px-4">
      <div className="w-full max-w-sm">
        <div className="text-center mb-8">
          <p className="text-emerald-400 font-semibold tracking-wide">HabitQuest</p>
          <h1 className="text-3xl font-bold mt-2">{title}</h1>
          <p className="text-slate-400 mt-2">{subtitle}</p>
        </div>

        <form onSubmit={handleSubmit} noValidate className="bg-slate-900 border border-slate-800 rounded-2xl p-6 space-y-4">
          {error && (
            <p role="alert" className="text-sm text-red-300 bg-red-950/50 border border-red-900 rounded-lg px-3 py-2">
              {error}
            </p>
          )}

          {children}

          <button
            type="submit"
            disabled={submitting}
            className="w-full rounded-lg bg-emerald-500 hover:bg-emerald-400 disabled:opacity-60 text-slate-950 font-semibold py-2.5 transition-colors"
          >
            {submitting ? 'Please wait…' : submitLabel}
          </button>
        </form>

        <p className="text-center text-sm text-slate-400 mt-6">{footer}</p>
      </div>
    </main>
  )
}

type TextFieldProps = {
  label: string
  type?: string
  value: string
  onChange: (value: string) => void
  error?: string
  autoComplete?: string
}

export function TextField({ label, type = 'text', value, onChange, error, autoComplete }: TextFieldProps) {
  return (
    <label className="block">
      <span className="text-sm text-slate-300">{label}</span>
      <input
        type={type}
        value={value}
        autoComplete={autoComplete}
        onChange={(e) => onChange(e.target.value)}
        aria-invalid={error ? true : undefined}
        className={`mt-1 w-full rounded-lg bg-slate-950 border px-3 py-2 outline-none focus:ring-2 focus:ring-emerald-500 ${
          error ? 'border-red-500' : 'border-slate-700'
        }`}
      />
      {error && <span className="text-xs text-red-300 mt-1 block">{error}</span>}
    </label>
  )
}
