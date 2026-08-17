import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { StorePage, StoreStatus } from '../Layout/StorePage'
import { Pagination } from '../ui/Pagination'
import { extractErrorMessage } from '../../services/api'
import { pedidoService } from '../../services/pedidoService'
import { StatusPedido, type Pedido } from '../../types/pedido'

export function SalesOrdersPage() {
  const [pedidos, setPedidos] = useState<Pedido[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)

  useEffect(() => {
    let active = true
    pedidoService
      .listarVendas(page, 10, StatusPedido.AGUARDANDO_CONFIRMACAO)
      .then((result) => {
        if (!active) return
        setPedidos(result.content.filter((pedido) => pedido.status === StatusPedido.AGUARDANDO_CONFIRMACAO))
        setTotalPages(result.totalPages)
        setError(null)
      })
      .catch((err) => {
        if (active) setError(extractErrorMessage(err, 'Nao foi possivel carregar as compras recebidas.'))
      })
      .finally(() => {
        if (active) setLoading(false)
      })

    return () => {
      active = false
    }
  }, [page])

  return (
    <StorePage title="Compras recebidas" subtitle="Pedidos aguardando confirmação do seu sebo.">
      {loading ? (
        <StoreStatus title="Carregando compras" description="Buscando pedidos enviados ao seu sebo." busy />
      ) : error ? (
        <StoreStatus title="Erro ao carregar compras" description={error} tone="error" />
      ) : pedidos.length === 0 ? (
        <StoreStatus
          title="Nenhuma compra aguardando confirmação"
          description="Quando clientes enviarem pedidos para seu sebo, eles aparecerão aqui."
          action={<Link to="/painel/produtos">Gerenciar produtos</Link>}
        />
      ) : (
        <section className="store-list" aria-label="Compras recebidas">
          {pedidos.map((pedido) => (
            <article className="store-order-card" key={pedido.id}>
              <header className="store-order-header">
                <div>
                  <h2>Pedido #{pedido.id}</h2>
                  <p>{new Date(pedido.createdAt).toLocaleDateString('pt-BR')} · R$ {Number(pedido.total).toFixed(2)}</p>
                </div>
                <span className="store-order-status">Aguardando confirmação</span>
              </header>
              <ul className="store-order-items">
                {pedido.itens.map((item) => (
                  <li key={item.id}>
                    {item.tituloSnapshot} · Qtd. {item.quantidade} · R$ {Number(item.precoSnapshot).toFixed(2)}
                  </li>
                ))}
              </ul>
              <div className="store-list-actions">
                <Link className="primary" to={`/vendas/${pedido.id}/confirmar`}>Responder pedido</Link>
              </div>
            </article>
          ))}
          <Pagination page={page} totalPages={totalPages} onPageChange={setPage} label="Páginas de compras" />
        </section>
      )}
    </StorePage>
  )
}
