import { useEffect, useRef, useState } from 'react'
import type { Produto } from '../types/produto'
import { ProductCard } from './ProductCard'
import { Link } from 'react-router-dom'
import './ProductCarousel.css'

type Props = {
  title: string
  produtos: Produto[]
  loading?: boolean
  placeholderCount?: number
  emptyMessage?: string
  viewAllTo?: string
  total?: number
}

function getVisibleProductCount(viewportWidth: number): number {
  if (viewportWidth <= 600) return 2
  if (viewportWidth <= 900) return 3
  return 5
}

export function ProductCarousel({ title, produtos, loading, placeholderCount = 5, emptyMessage, viewAllTo, total }: Props) {
  const trackRef = useRef<HTMLDivElement>(null)
  const [visibleProductCount, setVisibleProductCount] = useState(() => getVisibleProductCount(window.innerWidth))
  const items = loading
    ? Array.from({ length: placeholderCount }, (_, i) => ({ key: `skel-${i}`, produto: undefined }))
    : produtos.map((p) => ({ key: `p-${p.id}`, produto: p }))
  const showNavigation = !loading && produtos.length > visibleProductCount

  useEffect(() => {
    const updateVisibleProductCount = () => setVisibleProductCount(getVisibleProductCount(window.innerWidth))
    window.addEventListener('resize', updateVisibleProductCount)
    return () => window.removeEventListener('resize', updateVisibleProductCount)
  }, [])

  function scrollBy(direction: -1 | 1) {
    const node = trackRef.current
    if (!node) return
    node.scrollBy({ left: direction * node.clientWidth * 0.6, behavior: 'smooth' })
  }

  return (
    <section className="carousel" aria-busy={loading}>
      <div className="carousel-heading">
        <h2 className="section-title">{title}</h2>
        {viewAllTo && !loading && produtos.length > 0 && (total ?? produtos.length) > produtos.length
          ? <Link to={viewAllTo}>Ver todos</Link>
          : null}
      </div>
      {!loading && produtos.length === 0 ? (
        <p className="carousel-empty">{emptyMessage ?? 'Nenhum produto para exibir.'}</p>
      ) : (
      <div className="carousel-wrapper">
        {showNavigation ? (
          <button
            type="button"
            aria-label="Anterior"
            className="carousel-arrow left"
            onClick={() => scrollBy(-1)}
          >
            ‹
          </button>
        ) : null}
        <div className="carousel-track" ref={trackRef}>
          {items.map((item) => (
            <div className="carousel-item" key={item.key}>
              <ProductCard produto={item.produto} />
            </div>
          ))}
        </div>
        {showNavigation ? (
          <button
            type="button"
            aria-label="Próximo"
            className="carousel-arrow right"
            onClick={() => scrollBy(1)}
          >
            ›
          </button>
        ) : null}
      </div>
      )}
    </section>
  )
}
