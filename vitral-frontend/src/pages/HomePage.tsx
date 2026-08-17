import { useEffect, useMemo, useState } from 'react'
import { ProductCarousel } from '../components/ProductCarousel'
import { useAuth } from '../hooks/useAuth'
import { extractErrorMessage } from '../services/api'
import { homeService } from '../services/homeService'
import type { HomeResponse, HomeSection } from '../types/home'

type RenderSection = HomeSection & {
  key: string
  viewAllTo?: string
}

function hasContent(section: HomeSection | null | undefined): section is HomeSection {
  return Boolean(section && section.total > 0 && section.produtos.length > 0)
}

function buildSections(home: HomeResponse): RenderSection[] {
  const sections: RenderSection[] = []

  if (hasContent(home.lancamentos)) {
    sections.push({ ...home.lancamentos, key: 'lancamentos', viewAllTo: '/lancamentos' })
  }
  if (hasContent(home.classicos)) {
    sections.push({ ...home.classicos, key: 'classicos' })
  }
  if (hasContent(home.recomendados)) {
    sections.push({ ...home.recomendados, key: 'recomendados', viewAllTo: '/recomendacoes' })
  }

  home.categorias.forEach((category) => {
    if (category.total <= 0 || category.produtos.length === 0) return
    sections.push({
      titulo: category.nome,
      produtos: category.produtos,
      total: category.total,
      key: `categoria-${category.id}`,
      viewAllTo: `/categorias?categoriaId=${category.id}`,
    })
  })

  return sections
}

export function HomePage() {
  const { token } = useAuth()
  const [home, setHome] = useState<HomeResponse | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [reloadKey, setReloadKey] = useState(0)

  useEffect(() => {
    let active = true

    homeService.carregar()
      .then((response) => {
        if (!active) return
        setHome(response)
        setError(null)
      })
      .catch((err) => {
        if (!active) return
        setHome(null)
        setError(extractErrorMessage(err, 'Não foi possível carregar a vitrine.'))
      })
      .finally(() => {
        if (active) setLoading(false)
      })

    return () => { active = false }
  }, [token, reloadKey])

  const sections = useMemo(() => home ? buildSections(home) : [], [home])

  function retry() {
    setHome(null)
    setError(null)
    setLoading(true)
    setReloadKey((key) => key + 1)
  }

  return (
    <div className="page">
      <main className="container">
        {loading ? <ProductCarousel title="Carregando vitrine" produtos={[]} loading placeholderCount={5} /> : null}
        {!loading && error ? (
          <div className="flash error" role="alert">
            <span>{error}</span>{' '}
            <button type="button" onClick={retry}>Tentar novamente</button>
          </div>
        ) : null}
        {!loading && !error && sections.length === 0
          ? <p className="carousel-empty">Nenhum produto disponível no momento.</p>
          : null}
        {!loading && !error ? sections.map((section) => (
          <ProductCarousel
            key={section.key}
            title={section.titulo}
            produtos={section.produtos}
            total={section.total}
            viewAllTo={section.viewAllTo}
          />
        )) : null}
      </main>
    </div>
  )
}
