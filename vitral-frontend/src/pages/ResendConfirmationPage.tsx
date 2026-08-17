import { useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { Button } from '../components/ui/Button'
import { Input } from '../components/ui/Input'
import { extractErrorMessage } from '../services/api'
import { authService } from '../services/authService'
import './AuthPage.css'

export function ResendConfirmationPage() {
  const [params] = useSearchParams()
  const [email, setEmail] = useState(params.get('email') ?? '')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)

  async function submit(event: React.FormEvent) {
    event.preventDefault()
    setBusy(true)
    setError(null)
    try {
      const response = await authService.reenviarConfirmacao({ email })
      setSuccess(response.mensagem)
    } catch (err) {
      setError(extractErrorMessage(err, 'Não foi possível reenviar a confirmação.'))
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="page auth-page">
      <div className="auth-grid auth-grid-single">
        <form className="auth-card" onSubmit={submit}>
          <h1 className="auth-card-title">Reenviar confirmação</h1>
          <p className="auth-card-subtitle">Informe o e-mail usado no cadastro para receber um novo link.</p>
          {error ? <div className="flash error" role="alert">{error}</div> : null}
          {success ? <div className="flash success" role="status">{success}</div> : null}
          <Input id="confirmation-email" label="E-mail:" type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
          <Button type="submit" disabled={busy}>{busy ? 'Enviando...' : 'Reenviar link'}</Button>
        </form>
      </div>
    </div>
  )
}
