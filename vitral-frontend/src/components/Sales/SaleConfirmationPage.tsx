import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { StorePage, StoreStatus } from '../Layout/StorePage'
import { extractErrorMessage } from '../../services/api'
import { pedidoService } from '../../services/pedidoService'
import { StatusPedido, type Pedido } from '../../types/pedido'

export function SaleConfirmationPage() {
  const { orderId } = useParams()
  const numericOrderId = Number(orderId)
  const invalidOrderId = !Number.isFinite(numericOrderId)
  const [pedido, setPedido] = useState<Pedido | null>(null)
  const [loading, setLoading] = useState(!invalidOrderId)
  const [busy, setBusy] = useState<StatusPedido | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)

  useEffect(() => {
    let active = true
    if (invalidOrderId) return

    pedidoService
      .listarVendas()
      .then((page) => {
        if (!active) return
        setPedido(page.content.find((item) => item.id === numericOrderId) ?? null)
      })
      .catch((err) => {
        if (active) setError(extractErrorMessage(err, 'Nao foi possivel carregar o pedido.'))
      })
      .finally(() => {
        if (active) setLoading(false)
      })

    return () => {
      active = false
    }
  }, [invalidOrderId, numericOrderId])

  async function atualizarStatus(status: StatusPedido) {
    if (!pedido) return
    setBusy(status)
    setError(null)
    setSuccess(null)
    try {
      const atualizado = await pedidoService.atualizarStatus(pedido.id, status)
      setPedido(atualizado)
      setSuccess(status === StatusPedido.CONFIRMADO ? 'Pedido confirmado com sucesso.' : 'Pedido cancelado com sucesso.')
    } catch (err) {
      setError(extractErrorMessage(err, 'Nao foi possivel atualizar o pedido.'))
    } finally {
      setBusy(null)
    }
  }

  if (invalidOrderId) {
    return (
      <StorePage title="Confirmar compra" subtitle="Pedido inválido.">
        <StoreStatus
          title="Pedido inválido"
          description="Confira o link acessado e tente abrir o pedido novamente pela lista de vendas."
          tone="error"
          action={<Link to="/vendas">Voltar para vendas</Link>}
        />
      </StorePage>
    )
  }

  return (
    <StorePage title="Confirmar compra" subtitle="Confirme ou cancele um pedido recebido pelo seu sebo.">
      {loading ? (
        <StoreStatus title="Carregando pedido" description="Buscando os dados da venda." busy />
      ) : error && !pedido ? (
        <StoreStatus title="Erro ao carregar pedido" description={error} tone="error" />
      ) : !pedido ? (
        <StoreStatus
          title="Pedido não encontrado"
          description="Este pedido não foi encontrado entre as vendas do seu sebo."
          action={<Link to="/vendas">Voltar para vendas</Link>}
        />
      ) : (
        <article className="store-order-card">
          <header className="store-order-header">
            <div>
              <h2>Pedido #{pedido.id}</h2>
              <p>{new Date(pedido.createdAt).toLocaleDateString('pt-BR')} · R$ {Number(pedido.total).toFixed(2)}</p>
            </div>
            <span className="store-order-status">{pedido.status}</span>
          </header>

          {error ? <div className="flash error" role="alert">{error}</div> : null}
          {success ? <div className="flash success" role="status">{success}</div> : null}

          <ul className="store-order-items">
            {pedido.itens.map((item) => (
              <li key={item.id}>
                {item.tituloSnapshot} · Qtd. {item.quantidade} · R$ {Number(item.precoSnapshot).toFixed(2)}
              </li>
            ))}
          </ul>

          {pedido.status === StatusPedido.AGUARDANDO_CONFIRMACAO ? (
            <div className="store-list-actions">
              <button
                className="primary"
                type="button"
                disabled={Boolean(busy)}
                onClick={() => atualizarStatus(StatusPedido.CONFIRMADO)}
              >
                {busy === StatusPedido.CONFIRMADO ? 'Confirmando...' : 'Confirmar venda'}
              </button>
              <button
                className="danger"
                type="button"
                disabled={Boolean(busy)}
                onClick={() => atualizarStatus(StatusPedido.CANCELADO)}
              >
                {busy === StatusPedido.CANCELADO ? 'Cancelando...' : 'Recusar venda'}
              </button>
            </div>
          ) : (
            <div className="store-list-actions">
              <Link to="/vendas/historico">Ver histórico de vendas</Link>
            </div>
          )}
        </article>
      )}
    </StorePage>
  )
}
