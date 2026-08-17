import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { MinusCircleIcon } from '../Layout/Icons'
import { StorePage } from '../Layout/StorePage'
import { useAuthStore } from '../../store/authStore'
import { extractErrorMessage } from '../../services/api'
import { seboService } from '../../services/seboService'
import { usuarioService } from '../../services/usuarioService'
import './DeleteAccountPage.css'

export function DeleteAccountPage() {
  const navigate = useNavigate()
  const account = useAuthStore((state) => state.account)
  const isSebo = account?.type === 'SEBO'
  const forceLogout = useAuthStore((state) => state.forceLogout)
  const [confirmation, setConfirmation] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const canDelete = confirmation.trim().toUpperCase() === 'EXCLUIR'

  async function excluirConta() {
    if (!canDelete) return
    setBusy(true)
    setError(null)
    try {
      if (isSebo) {
        await seboService.excluirConta()
      } else {
        await usuarioService.excluirConta()
      }
      forceLogout()
      navigate('/auth')
    } catch (err) {
      setError(extractErrorMessage(err, 'Nao foi possivel excluir sua conta.'))
    } finally {
      setBusy(false)
    }
  }

  return (
    <StorePage title="Excluir conta" subtitle="Revise as consequências antes de continuar.">
      <section className="delete-account">
        <div className="delete-warning">
          <MinusCircleIcon size={52} />
          <h2>Sua conta será desativada.</h2>
          <p>Seus dados pessoais serão anonimizados e você perderá o acesso ao Vitral.</p>
          <ul>
            <li>Favoritos, cesta e informações pessoais serão removidos.</li>
            {isSebo ? <li>Produtos, perfil do sebo e dados vinculados ao sebo serão excluídos.</li> : null}
            <li>Pedidos, movimentações de estoque e registros financeiros serão preservados para integridade e auditoria.</li>
          </ul>
        </div>

        <div className="delete-confirmation">
          <h2>Confirmar exclusão</h2>
          <p>
            Você está excluindo a conta {account?.email}. Digite <strong>EXCLUIR</strong> para confirmar.
          </p>
          {error ? <div className="flash error" role="alert">{error}</div> : null}
          <label htmlFor="delete-confirmation">
            Confirmação
            <input
              id="delete-confirmation"
              value={confirmation}
              onChange={(event) => setConfirmation(event.target.value)}
              placeholder="Digite EXCLUIR"
            />
          </label>
          <button type="button" disabled={!canDelete || busy} onClick={excluirConta}>
            {busy ? 'Excluindo...' : 'Excluir conta'}
          </button>
          <Link to="/painel/perfil">Cancelar e voltar</Link>
        </div>
      </section>
    </StorePage>
  )
}
