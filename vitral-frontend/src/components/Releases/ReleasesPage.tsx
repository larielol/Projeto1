import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { ProductCard } from '../ProductCard'
import { StorePage, StoreStatus } from '../Layout/StorePage'
import { extractErrorMessage } from '../../services/api'
import { produtoService } from '../../services/produtoService'
import type { Produto } from '../../types/produto'

export function ReleasesPage() {
  const [products, setProducts] = useState<Produto[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let active = true
    produtoService
      .listarLancamentos()
      .then((page) => {
        if (!active) return
        setProducts(page.content)
        setError(null)
      })
      .catch((err) => {
        if (active) setError(extractErrorMessage(err, 'Nao foi possivel carregar os lançamentos.'))
      })
      .finally(() => {
        if (active) setLoading(false)
      })

    return () => {
      active = false
    }
  }, [])

  return (
    <StorePage title="Lançamentos" subtitle="As novidades que acabaram de chegar ao Vitral.">
      {loading ? (
        <StoreStatus title="Carregando lançamentos" description="Buscando os produtos mais recentes." busy />
      ) : error ? (
        <StoreStatus title="Erro ao carregar lançamentos" description={error} tone="error" />
      ) : products.length === 0 ? (
        <StoreStatus
          title="Nenhum lançamento disponível"
          description="Quando os sebos cadastrarem novos produtos, eles aparecerão aqui."
          action={<Link to="/busca">Ver acervo</Link>}
        />
      ) : (
        <section className="busca-grid" aria-label="Lançamentos recentes">
          {products.map((product) => (
            <ProductCard key={product.id} produto={product} />
          ))}
        </section>
      )}
    </StorePage>
  )
}
