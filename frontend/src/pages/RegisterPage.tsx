import { useState } from 'react'
import { Link, useNavigate } from 'react-router'
import { useAuth } from '../auth/AuthContext'
import { AuthCard, TextField } from '../components/AuthCard'
import { ApiError } from '../lib/api'

export function RegisterPage() {
  const { register } = useAuth()
  const navigate = useNavigate()
  const [displayName, setDisplayName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({})
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit() {
    setError(null)
    setFieldErrors({})
    setSubmitting(true)
    try {
      await register(email, password, displayName)
      navigate('/', { replace: true })
    } catch (e) {
      if (e instanceof ApiError) {
        setFieldErrors(e.fieldErrors)
        // Field problems are shown under each input; show the banner only for other errors.
        if (Object.keys(e.fieldErrors).length === 0) setError(e.message)
      } else {
        setError('Something went wrong')
      }
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <AuthCard
      title="Start your quest"
      subtitle="Track habits, earn points, claim rewards."
      error={error}
      submitting={submitting}
      submitLabel="Create account"
      onSubmit={handleSubmit}
      footer={<>Already have an account? <Link to="/login" className="text-emerald-400 hover:underline">Log in</Link></>}
    >
      <TextField label="Display name" value={displayName} onChange={setDisplayName} error={fieldErrors.displayName} autoComplete="nickname" />
      <TextField label="Email" type="email" value={email} onChange={setEmail} error={fieldErrors.email} autoComplete="email" />
      <TextField label="Password (8+ characters)" type="password" value={password} onChange={setPassword} error={fieldErrors.password} autoComplete="new-password" />
    </AuthCard>
  )
}
