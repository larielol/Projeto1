import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { Button } from '../components/ui/Button'
import { Input } from '../components/ui/Input'
import { authService } from '../services/authService'
import { seboService } from '../services/seboService'
import { extractErrorMessage } from '../services/api'
import { useAuth } from '../hooks/useAuth'
import { AccountType } from '../types/account'
import './AuthPage.css'

export function AuthPage() {
  const navigate = useNavigate()
  const { login, setSeboId, sessionMessage, clearSessionMessage } = useAuth()

  const [loginIdentifier, setLoginIdentifier] = useState('')
  const [loginPassword, setLoginPassword] = useState('')
  const [loginError, setLoginError] = useState<string | null>(null)
  const [loginBusy, setLoginBusy] = useState(false)

  const [regName, setRegName] = useState('')
  const [regUsername, setRegUsername] = useState('')
  const [regEmail, setRegEmail] = useState('')
  const [regPassword, setRegPassword] = useState('')
  const [regConfirm, setRegConfirm] = useState('')
  const [regType, setRegType] = useState<AccountType>(AccountType.USUARIO)
  const [regError, setRegError] = useState<string | null>(null)
  const [regBusy, setRegBusy] = useState(false)
  const [regSuccess, setRegSuccess] = useState<string | null>(null)

  function sanitizeUsername(valor: string) {
    return valor
      .toLowerCase()
      .normalize('NFD')
      .replace(/[̀-ͯ]/g, '')
      .replace(/\s+/g, '.')
      .replace(/[^a-z0-9._-]/g, '')
      .slice(0, 30)
  }

  async function handleLogin(e: React.FormEvent) {
    e.preventDefault()
    setLoginError(null)
    setLoginBusy(true)
    try {
      const res = await authService.login({ login: loginIdentifier, password: loginPassword })
      login(res.token, res.account)
      if (res.account.type === AccountType.SEBO) {
        try {
          const sebo = await seboService.buscarMeu()
          setSeboId(sebo.id)
        } catch {
          // Ainda nao existe perfil cadastrado; o usuario pode criar no painel.
        }
      }
      navigate('/')
    } catch (err) {
      setLoginError(extractErrorMessage(err, 'Falha ao entrar'))
    } finally {
      setLoginBusy(false)
    }
  }

  async function handleRegister(e: React.FormEvent) {
    e.preventDefault()
    setRegError(null)
    if (regPassword !== regConfirm) {
      setRegError('As senhas nao coincidem')
      return
    }
    if (regPassword.length < 8) {
      setRegError('A senha deve ter ao menos 8 caracteres')
      return
    }
    setRegBusy(true)
    try {
      const res = await authService.register({
        name: regName,
        username: regUsername,
        email: regEmail,
        password: regPassword,
        type: regType,
      })
      setRegSuccess(res.mensagem)
    } catch (err) {
      setRegError(extractErrorMessage(err, 'Falha ao cadastrar'))
    } finally {
      setRegBusy(false)
    }
  }

  return (
    <div className="page auth-page">
      <div className="auth-grid">
        <form className="auth-card" onSubmit={handleLogin}>
          {sessionMessage ? (
            <div className="flash error" role="alert">
              {sessionMessage}
              <button type="button" className="auth-message-dismiss" onClick={clearSessionMessage}>Fechar</button>
            </div>
          ) : null}
          {loginError ? <div className="flash error" role="alert">{loginError}</div> : null}
          <Input
            id="login-identifier"
            label="Usuário ou E-mail:"
            type="text"
            value={loginIdentifier}
            onChange={(e) => setLoginIdentifier(e.target.value)}
            required
          />
          <Input
            id="login-password"
            label="Senha:"
            type="password"
            value={loginPassword}
            onChange={(e) => setLoginPassword(e.target.value)}
            required
          />
          <Button type="submit" disabled={loginBusy}>
            {loginBusy ? 'Entrando...' : 'Entrar'}
          </Button>
          <Link to="/auth/recuperar-senha" className="auth-forgot-link">Esqueci minha senha</Link>
          <Link to="/auth/reenviar-confirmacao" className="auth-forgot-link">Reenviar confirmação</Link>
        </form>

        {regSuccess ? (
          <div className="auth-card auth-success-card">
            <div className="flash success" role="status">{regSuccess}</div>
            <p className="auth-card-subtitle">Verifique sua caixa de entrada e clique no link de confirmação para ativar sua conta.</p>
            <Link
              to={`/auth/reenviar-confirmacao?email=${encodeURIComponent(regEmail)}`}
              className="auth-forgot-link"
            >
              Não recebeu? Reenviar confirmação
            </Link>
            <Button type="button" onClick={() => setRegSuccess(null)}>
              Cadastrar outra conta
            </Button>
          </div>
        ) : (
          <form className="auth-card" onSubmit={handleRegister}>
            <p className="auth-card-title">Ainda não possui conta?</p>
            <p className="auth-card-subtitle"><em>Cadastra-se:</em></p>
            {regError ? <div className="flash error" role="alert">{regError}</div> : null}
            <Input
              id="reg-name"
              label="Nome:"
              placeholder="Como você quer ser chamado no Vitral"
              value={regName}
              onChange={(e) => setRegName(e.target.value)}
              required
            />
            <Input
              id="reg-username"
              label="Usuário (login):"
              placeholder="apenas letras minúsculas, números, ponto, hífen ou underline"
              value={regUsername}
              onChange={(e) => setRegUsername(sanitizeUsername(e.target.value))}
              minLength={3}
              maxLength={30}
              required
            />
            <Input
              id="reg-email"
              label="Email:"
              type="email"
              value={regEmail}
              onChange={(e) => setRegEmail(e.target.value)}
              required
            />
            <Input
              id="reg-password"
              label="Senha:"
              type="password"
              placeholder="Necessário ao menos 8 caracteres"
              value={regPassword}
              onChange={(e) => setRegPassword(e.target.value)}
              required
            />
            <Input
              id="reg-confirm"
              label="Confirme sua senha:"
              type="password"
              placeholder="Repita a senha"
              value={regConfirm}
              onChange={(e) => setRegConfirm(e.target.value)}
              required
            />
            <div className="auth-type-toggle">
              <label>
                <input
                  type="radio"
                  name="account-type"
                  value={AccountType.USUARIO}
                  checked={regType === AccountType.USUARIO}
                  onChange={() => setRegType(AccountType.USUARIO)}
                />
                <span>Sou cliente</span>
              </label>
              <label>
                <input
                  type="radio"
                  name="account-type"
                  value={AccountType.SEBO}
                  checked={regType === AccountType.SEBO}
                  onChange={() => setRegType(AccountType.SEBO)}
                />
                <span>Sou um sebo</span>
              </label>
            </div>
            <Button type="submit" disabled={regBusy}>
              {regBusy ? 'Cadastrando...' : 'Cadastrar'}
            </Button>
          </form>
        )}
      </div>
    </div>
  )
}
