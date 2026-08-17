import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { ProductCard } from '../components/ProductCard'
import { StorePage, StoreStatus } from '../components/Layout/StorePage'
import { Pagination } from '../components/ui/Pagination'
import { Button } from '../components/ui/Button'
import { extractErrorMessage } from '../services/api'
import { recomendacaoService } from '../services/recomendacaoService'
import type { Produto } from '../types/produto'
import './RecomendacoesPage.css'

export function RecomendacoesPage() {
  const [products, setProducts] = useState<Produto[]>([])
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [loading, setLoading] = useState(true)
  const [clearing, setClearing] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)

  useEffect(() => {
    let active = true
    recomendacaoService.listar(page, 20)
      .then((result) => {
        if (!active) return
        setProducts(result.content)
        setTotalPages(result.totalPages)
        setError(null)
      })
      .catch((err) => {
        if (!active) return
        setProducts([])
        setTotalPages(0)
        setError(extractErrorMessage(err, 'Não foi possível carregar suas recomendações.'))
      })
      .finally(() => {
        if (active) setLoading(false)
      })
    return () => { active = false }
  }, [page])

  async function clearHistory() {
    if (!window.confirm('Deseja limpar os dados usados para personalizar suas recomendações?')) return
    setClearing(true)
    setError(null)
    setSuccess(null)
    try {
      const response = await recomendacaoService.limparHistorico()
      setProducts([])
      setTotalPages(0)
      setPage(0)
      setSuccess(response.mensagem)
    } catch (err) {
      setError(extractErrorMessage(err, 'Não foi possível limpar o histórico de recomendações.'))
    } finally {
      setClearing(false)
    }
  }

  function changePage(nextPage: number) {
    setLoading(true)
    setPage(nextPage)
  }

  return (
    <StorePage title="Recomendados para você" subtitle="Sugestões baseadas nas suas interações com o acervo do Vitral.">
      <div className="recommendations-actions">
        <Button type="button" variant="ghost" disabled={clearing} onClick={clearHistory}>
          {clearing ? 'Limpando...' : 'Limpar histórico de recomendações'}
        </Button>
      </div>
      {success ? <div className="flash success" role="status">{success}</div> : null}
      {loading ? (
        <StoreStatus title="Carregando recomendações" description="Preparando sugestões para você." busy />
      ) : error ? (
        <StoreStatus title="Erro ao carregar recomendações" description={error} tone="error" />
      ) : products.length === 0 ? (
        <StoreStatus
          title="Ainda não há recomendações"
          description="Pesquise, visualize ou favorite produtos para receber sugestões personalizadas."
          action={<Link to="/busca">Explorar o acervo</Link>}
        />
      ) : (
        <>
          <section className="recommendations-grid" aria-label="Produtos recomendados">
            {products.map((product) => <ProductCard key={product.id} produto={product} />)}
          </section>
          <Pagination page={page} totalPages={totalPages} onPageChange={changePage} label="Páginas de recomendações" />
        </>
      )}
    </StorePage>
  )
}
