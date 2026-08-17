import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { StorePage, StoreStatus } from '../Layout/StorePage'
import { extractErrorMessage } from '../../services/api'
import { favoritoService } from '../../services/favoritoService'
import type { Favorito } from '../../types/favorito'

export function FavoritesPage() {
  const [favoritos, setFavoritos] = useState<Favorito[]>([])
  const [loading, setLoading] = useState(true)
  const [busyId, setBusyId] = useState<number | null>(null)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let active = true
    favoritoService
      .listar()
      .then((items) => {
        if (!active) return
        setFavoritos(items)
        setError(null)
      })
      .catch((err) => {
        if (!active) return
        setFavoritos([])
        setError(extractErrorMessage(err, 'Nao foi possivel carregar seus favoritos.'))
      })
      .finally(() => {
        if (active) setLoading(false)
      })

    return () => {
      active = false
    }
  }, [])

  async function remover(produtoId: number) {
    setBusyId(produtoId)
    setError(null)
    try {
      await favoritoService.remover(produtoId)
      setFavoritos((items) => items.filter((item) => item.produtoId !== produtoId))
    } catch (err) {
      setError(extractErrorMessage(err, 'Nao foi possivel remover o favorito.'))
    } finally {
      setBusyId(null)
    }
  }

  return (
    <StorePage
      title="Favoritos"
      subtitle="Os itens que você guardou para encontrar depois."
      searchPlaceholder="Pesquisar nos favoritos"
    >
      {loading ? (
        <StoreStatus title="Carregando favoritos" description="Buscando seus itens salvos." busy />
      ) : error ? (
        <StoreStatus title="Erro ao carregar favoritos" description={error} tone="error" />
      ) : favoritos.length === 0 ? (
        <StoreStatus
          title="Sua lista está vazia"
          description="Quando encontrar algo especial, salve nos favoritos para voltar depois."
          action={<Link to="/busca">Explorar o Vitral</Link>}
        />
      ) : (
        <section className="store-list" aria-label="Produtos favoritos">
          {favoritos.map((item) => (
            <article className="store-list-card" key={item.id}>
              <Link className="store-list-image" to={`/produto/${item.produtoId}`}>
                {item.fotoUrl ? <img src={item.fotoUrl} alt={item.titulo} /> : 'Sem imagem'}
              </Link>
              <div className="store-list-copy">
                <h2>{item.titulo}</h2>
                <p>{item.autor ?? 'Autor nao informado'} · {item.condicao}</p>
                <strong className="store-list-price">
                  {item.precoPromocional != null && Number(item.precoPromocional) < Number(item.preco) ? (
                    <>
                      <s style={{ opacity: 0.6, marginRight: 6, fontWeight: 400 }}>
                        R$ {Number(item.preco).toFixed(2)}
                      </s>
                      R$ {Number(item.precoPromocional).toFixed(2)}
                    </>
                  ) : (
                    <>R$ {Number(item.preco).toFixed(2)}</>
                  )}
                </strong>
              </div>
              <div className="store-list-actions">
                <Link to={`/produto/${item.produtoId}`} className="primary">Ver produto</Link>
                <button
                  className="danger"
                  type="button"
                  disabled={busyId === item.produtoId}
                  onClick={() => remover(item.produtoId)}
                >
                  {busyId === item.produtoId ? 'Removendo...' : 'Remover'}
                </button>
              </div>
            </article>
          ))}
        </section>
      )}
    </StorePage>
  )
}
