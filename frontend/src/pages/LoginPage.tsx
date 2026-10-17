import { useEffect, useRef, useState } from 'react'
import { Link, useLocation, useNavigate } from 'react-router'
import { useAuth } from '../auth/AuthContext'
import { AuthCard, TextField } from '../components/AuthCard'
import { isFirstScreen, markOtherScreenShown } from '../intro/firstScreen'
import { INTRO_DEBUG, introLog, introLogText } from '../intro/introDebug'
import { PandaIntro } from '../intro/PandaIntro'
import { ApiError } from '../lib/api'

export function LoginPage() {
  const { login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)

  // The panda intro plays only when this is the first screen of a visit, and not for people who asked for less motion.
  const [introOff] = useState(() => {
    const reason = !isFirstScreen() ? 'not the first screen of this visit'
      : window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 'Reduce Motion is on'
        : null
    if (reason) introLog(`intro off: ${reason}`)
    return reason
  })
  const [showIntro, setShowIntro] = useState(introOff === null)
  const cardRef = useRef<HTMLDivElement>(null)
  // Coming back to this page later in the same visit (e.g. from sign-up) shouldn't replay it.
  useEffect(() => markOtherScreenShown, [])

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
      cardRef={cardRef}
      overlay={showIntro
        ? <PandaIntro cardRef={cardRef} onDone={() => setShowIntro(false)} />
        : INTRO_DEBUG && (
          <pre className="fixed left-2 top-2 z-30 max-w-[90vw] whitespace-pre-wrap rounded bg-black/80 p-2 text-[11px] text-yellow-300">
            {introLogText() || 'intro: nothing logged'}
          </pre>
        )}
    >
      <TextField label="Email" type="email" value={email} onChange={setEmail} autoComplete="email" />
      <TextField label="Password" type="password" value={password} onChange={setPassword} autoComplete="current-password" />
    </AuthCard>
  )
}
