import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { StorePage, StoreStatus } from '../Layout/StorePage'
import { extractErrorMessage } from '../../services/api'
import { ofertaService } from '../../services/ofertaService'
import type { Oferta } from '../../types/oferta'

export function OffersPage() {
  const [offers, setOffers] = useState<Oferta[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let active = true
    ofertaService
      .listarAtivas()
      .then((page) => {
        if (!active) return
        setOffers(page.content)
        setError(null)
      })
      .catch((err) => {
        if (active) setError(extractErrorMessage(err, 'Nao foi possivel carregar as ofertas.'))
      })
      .finally(() => {
        if (active) setLoading(false)
      })

    return () => {
      active = false
    }
  }, [])

  return (
    <StorePage title="Ofertas" subtitle="Itens em promoção publicados pelos sebos.">
      {loading ? (
        <StoreStatus title="Carregando ofertas" description="Buscando as ofertas disponíveis agora." busy />
      ) : error ? (
        <StoreStatus title="Erro ao carregar ofertas" description={error} tone="error" />
      ) : offers.length === 0 ? (
        <StoreStatus
          title="Nenhuma oferta ativa"
          description="Quando os sebos cadastrarem promoções, elas aparecerão aqui."
          action={<Link to="/busca">Buscar produtos</Link>}
        />
      ) : (
        <section className="store-list" aria-label="Ofertas ativas">
          {offers.map((offer) => {
            const desconto = 100 - (Number(offer.precoPromocional) / Number(offer.precoOriginal)) * 100
            return (
              <article className="store-list-card" key={offer.id}>
                <Link className="store-list-image" to={`/produto/${offer.produtoId}`}>
                  {Math.max(0, Math.round(desconto))}% off
                </Link>
                <div className="store-list-copy">
                  <h2>{offer.tituloProduto}</h2>
                  {offer.descricao ? <p>{offer.descricao}</p> : <p>Oferta ativa</p>}
                  <span>De R$ {Number(offer.precoOriginal).toFixed(2)}</span>
                  <strong className="store-list-price">Por R$ {Number(offer.precoPromocional).toFixed(2)}</strong>
                </div>
                <div className="store-list-actions">
                  <Link to={`/produto/${offer.produtoId}`} className="primary">Ver produto</Link>
                </div>
              </article>
            )
          })}
        </section>
      )}
    </StorePage>
  )
}
