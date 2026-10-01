import { useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router'
import { useAuth } from '../auth/AuthContext'
import { AuthCard, TextField } from '../components/AuthCard'
import { ApiError } from '../lib/api'

export function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  async function handleSubmit() {
    setError(null)
    setSubmitting(true)
    try {
      await login(email, password)
      // Go back to the page the user originally wanted, or home.
      navigate(location.state?.from ?? '/', { replace: true })
    } catch (e) {
      setError(e instanceof ApiError ? e.message : 'Something went wrong')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <AuthCard
      title="Welcome back"
      subtitle="Log in to keep your streaks going."
      error={error}
      submitting={submitting}
      submitLabel="Log in"
      onSubmit={handleSubmit}
      footer={<>New here? <Link to="/register" className="text-emerald-400 hover:underline">Create an account</Link></>}
    >
      <TextField label="Email" type="email" value={email} onChange={setEmail} autoComplete="email" />
      <TextField label="Password" type="password" value={password} onChange={setPassword} autoComplete="current-password" />
    </AuthCard>
  )
}
