import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { StorePage, StoreStatus } from '../Layout/StorePage'
import { Pagination } from '../ui/Pagination'
import { extractErrorMessage } from '../../services/api'
import { pedidoService } from '../../services/pedidoService'
import { StatusPedido, type Pedido } from '../../types/pedido'

function statusLabel(status: string) {
  if (status === StatusPedido.AGUARDANDO_CONFIRMACAO) return 'Aguardando confirmação'
  if (status === StatusPedido.CONFIRMADO) return 'Confirmado'
  return 'Cancelado'
}

export function ReservationsPage() {
  const [pedidos, setPedidos] = useState<Pedido[]>([])
  const [loading, setLoading] = useState(true)
  const [busyId, setBusyId] = useState<number | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)

  useEffect(() => {
    let active = true
    pedidoService
      .listarMeusPedidos(page, 10, StatusPedido.AGUARDANDO_CONFIRMACAO)
      .then((result) => {
        if (!active) return
        setPedidos(result.content.filter((pedido) => pedido.status === StatusPedido.AGUARDANDO_CONFIRMACAO))
        setTotalPages(result.totalPages)
        setError(null)
      })
      .catch((err) => {
        if (active) setError(extractErrorMessage(err, 'Nao foi possivel carregar suas reservas.'))
      })
      .finally(() => {
        if (active) setLoading(false)
      })

    return () => {
      active = false
    }
  }, [page])

  async function cancelarPedido(id: number) {
    setBusyId(id)
    setError(null)
    try {
      await pedidoService.cancelarPedido(id)
      setPedidos((current) => current.filter((pedido) => pedido.id !== id))
    } catch (err) {
      setError(extractErrorMessage(err, 'Nao foi possivel cancelar o pedido.'))
    } finally {
      setBusyId(null)
    }
  }

  return (
    <StorePage title="Minhas reservas" subtitle="Acompanhe os pedidos aguardando confirmação do sebo.">
      {loading ? (
        <StoreStatus title="Carregando reservas" description="Buscando seus pedidos em aberto." busy />
      ) : error ? (
        <StoreStatus title="Erro ao carregar reservas" description={error} tone="error" />
      ) : pedidos.length === 0 ? (
        <StoreStatus
          title="Nenhuma reserva em aberto"
          description="Pedidos enviados aos sebos e ainda não confirmados aparecerão aqui."
          action={<Link to="/busca">Buscar produtos</Link>}
        />
      ) : (
        <section className="store-list" aria-label="Reservas em aberto">
          {pedidos.map((pedido) => (
            <article className="store-order-card" key={pedido.id}>
              <header className="store-order-header">
                <div>
                  <h2>Pedido #{pedido.id}</h2>
                  <p>{new Date(pedido.createdAt).toLocaleDateString('pt-BR')} · R$ {Number(pedido.total).toFixed(2)}</p>
                </div>
                <span className="store-order-status">{statusLabel(pedido.status)}</span>
              </header>
              <ul className="store-order-items">
                {pedido.itens.map((item) => (
                  <li key={item.id}>
                    {item.tituloSnapshot} · Qtd. {item.quantidade} · R$ {Number(item.precoSnapshot).toFixed(2)}
                  </li>
                ))}
              </ul>
              <div className="store-list-actions">
                <button
                  className="danger"
                  type="button"
                  disabled={busyId === pedido.id}
                  onClick={() => cancelarPedido(pedido.id)}
                >
                  {busyId === pedido.id ? 'Cancelando...' : 'Cancelar pedido'}
                </button>
              </div>
            </article>
          ))}
          <Pagination page={page} totalPages={totalPages} onPageChange={setPage} label="Páginas de reservas" />
        </section>
      )}
    </StorePage>
  )
}
