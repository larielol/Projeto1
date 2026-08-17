import { useEffect, useRef, useState } from 'react'
import { Link, useNavigate, useSearchParams } from 'react-router-dom'
import { authService } from '../services/authService'
import { extractErrorMessage } from '../services/api'
import './ConfirmarEmailPage.css'

type Estado = 'carregando' | 'sucesso' | 'erro'

const REDIRECT_DELAY = 4

export function ConfirmarEmailPage() {
  const [searchParams] = useSearchParams()
  const navigate = useNavigate()
  const token = searchParams.get('token')

  const [estado, setEstado] = useState<Estado>(token ? 'carregando' : 'erro')
  const [mensagem, setMensagem] = useState(token ? '' : 'Link de confirmação inválido.')
  const [contador, setContador] = useState(REDIRECT_DELAY)
  const intervalRef = useRef<ReturnType<typeof setInterval> | null>(null)
  const confirmadoRef = useRef(false)

  useEffect(() => {
    if (!token || confirmadoRef.current) return
    confirmadoRef.current = true

    authService
      .confirmarEmail(token)
      .then((res) => {
        setMensagem(res.mensagem)
        setEstado('sucesso')
      })
      .catch((err) => {
        setMensagem(extractErrorMessage(err, 'Não foi possível confirmar o e-mail.'))
        setEstado('erro')
      })
  }, [token])

  useEffect(() => {
    if (estado !== 'sucesso') return

    intervalRef.current = setInterval(() => {
      setContador((prev) => {
        if (prev <= 1) {
          clearInterval(intervalRef.current!)
          navigate('/auth', { replace: true })
          return 0
        }
        return prev - 1
      })
    }, 1000)

    return () => {
      if (intervalRef.current) clearInterval(intervalRef.current)
    }
  }, [estado, navigate])

  return (
    <div className="confirmar-email-page">
      <div className="confirmar-email-card">
        {estado === 'carregando' ? (
          <div role="status">
            <div className="confirmar-email-icon">⏳</div>
            <h2>Confirmando seu e-mail...</h2>
          </div>
        ) : estado === 'sucesso' ? (
          <>
            <div className="confirmar-email-icon">✓</div>
            <h2>E-mail confirmado!</h2>
            <div className="flash success" role="status">{mensagem}</div>
            <p className="confirmar-email-hint">
              Redirecionando para o login em {contador} segundo{contador !== 1 ? 's' : ''}...
            </p>
            <Link to="/auth" className="confirmar-email-btn">Ir para o login agora</Link>
          </>
        ) : (
          <>
            <div className="confirmar-email-icon">!</div>
            <h2>Confirmação falhou</h2>
            <div className="flash error" role="alert">{mensagem}</div>
            <p className="confirmar-email-hint">
              O link pode ter expirado ou já ter sido utilizado.
            </p>
            <Link to="/auth" className="confirmar-email-btn">Voltar para o login</Link>
            <Link to="/auth/reenviar-confirmacao" className="confirmar-email-btn">Solicitar novo link</Link>
          </>
        )}
      </div>
    </div>
  )
}
