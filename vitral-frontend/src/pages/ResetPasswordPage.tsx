import { useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { Button } from '../components/ui/Button'
import { Input } from '../components/ui/Input'
import { extractErrorMessage } from '../services/api'
import { authService } from '../services/authService'
import './AuthPage.css'

export function ResetPasswordPage() {
  const [params] = useSearchParams()
  const token = params.get('token') ?? ''
  const [password, setPassword] = useState('')
  const [confirmation, setConfirmation] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)

  async function submit(event: React.FormEvent) {
    event.preventDefault()
    setError(null)
    if (!token) return setError('O link de recuperação é inválido.')
    if (password.length < 8) return setError('A senha deve ter ao menos 8 caracteres.')
    if (password !== confirmation) return setError('As senhas não coincidem.')
    setBusy(true)
    try {
      const response = await authService.redefinirSenha({ token, password })
      setSuccess(response.mensagem)
    } catch (err) {
      setError(extractErrorMessage(err, 'Não foi possível redefinir sua senha.'))
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="page auth-page">
      <div className="auth-grid auth-grid-single">
        <form className="auth-card" onSubmit={submit}>
          <h1 className="auth-card-title">Criar nova senha</h1>
          {error ? <div className="flash error" role="alert">{error}</div> : null}
          {success ? <div className="flash success" role="status">{success} <Link to="/auth">Entrar</Link></div> : null}
          <Input id="new-password" label="Nova senha:" type="password" value={password} onChange={(e) => setPassword(e.target.value)} required />
          <Input id="confirm-new-password" label="Confirme a nova senha:" type="password" value={confirmation} onChange={(e) => setConfirmation(e.target.value)} required />
          <Button type="submit" disabled={busy || Boolean(success)}>{busy ? 'Salvando...' : 'Redefinir senha'}</Button>
        </form>
      </div>
    </div>
  )
}
