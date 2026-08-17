import { useState } from 'react'
import { Button } from '../components/ui/Button'
import { Input } from '../components/ui/Input'
import { extractErrorMessage } from '../services/api'
import { authService } from '../services/authService'
import './AuthPage.css'

export function ForgotPasswordPage() {
  const [email, setEmail] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)

  async function submit(event: React.FormEvent) {
    event.preventDefault()
    setBusy(true)
    setError(null)
    try {
      const response = await authService.solicitarRecuperacao({ email })
      setSuccess(response.mensagem)
    } catch (err) {
      setError(extractErrorMessage(err, 'Não foi possível solicitar a recuperação.'))
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="page auth-page">
      <div className="auth-grid auth-grid-single">
        <form className="auth-card" onSubmit={submit}>
          <h1 className="auth-card-title">Recuperar senha</h1>
          <p className="auth-card-subtitle">Enviaremos um link de redefinição para o e-mail cadastrado.</p>
          {error ? <div className="flash error" role="alert">{error}</div> : null}
          {success ? <div className="flash success" role="status">{success}</div> : null}
          <Input id="recovery-email" label="E-mail:" type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
          <Button type="submit" disabled={busy || Boolean(success)}>{busy ? 'Enviando...' : 'Enviar link'}</Button>
        </form>
      </div>
    </div>
  )
}
