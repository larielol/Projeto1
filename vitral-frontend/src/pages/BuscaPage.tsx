import { useEffect, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { ProductCard } from '../components/ProductCard'
import { Pagination } from '../components/ui/Pagination'
import { extractErrorMessage } from '../services/api'
import { buscaService } from '../services/buscaService'
import type { Produto } from '../types/produto'
import type { Sebo } from '../types/sebo'
import { CondicaoProduto } from '../types/produto'
import './BuscaPage.css'
import { categoriaService } from '../services/categoriaService'
import type { Categoria } from '../types/categoria'
import { BOOK_GENRE_OPTIONS, getAllowedCategories, isBookCategory, type BookGenre } from '../constants/productCatalog'

type Tab = 'produtos' | 'sebos'

export function BuscaPage() {
  const [searchParams, setSearchParams] = useSearchParams()

  const tab = (searchParams.get('tab') as Tab) ?? 'produtos'
  const q = searchParams.get('q') ?? ''
  const appliedCondicao = searchParams.get('condicao') ?? ''
  const appliedSeboId = searchParams.get('seboId') ?? ''
  const appliedPrecoMin = searchParams.get('precoMin') ?? ''
  const appliedPrecoMax = searchParams.get('precoMax') ?? ''
  const appliedCategoriaId = searchParams.get('categoriaId') ?? ''
  const appliedBookGenre = searchParams.get('bookGenre') ?? ''
  const appliedCidade = searchParams.get('cidade') ?? ''
  const appliedUf = searchParams.get('uf') ?? ''
  const requestedPage = Number(searchParams.get('page') ?? '1')
  const currentPage = Number.isInteger(requestedPage) && requestedPage > 0 ? requestedPage - 1 : 0

  const [inputQ, setInputQ] = useState(q)
  const [condicao, setCondicao] = useState(appliedCondicao)
  const [seboId, setSeboId] = useState(appliedSeboId)
  const [precoMin, setPrecoMin] = useState(appliedPrecoMin)
  const [precoMax, setPrecoMax] = useState(appliedPrecoMax)
  const [categoriaId, setCategoriaId] = useState(appliedCategoriaId)
  const [bookGenre, setBookGenre] = useState(appliedBookGenre)
  const [categorias, setCategorias] = useState<Categoria[]>([])
  const [cidade, setCidade] = useState(appliedCidade)
  const [uf, setUf] = useState(appliedUf)

  const [proximidadeAtiva, setProximidadeAtiva] = useState(false)
  const [coords, setCoords] = useState<{ lat: number; lng: number } | null>(null)
  const [geoLoading, setGeoLoading] = useState(false)
  const [geoError, setGeoError] = useState<string | null>(null)

  const [produtos, setProdutos] = useState<Produto[]>([])
  const [sebos, setSebos] = useState<Sebo[]>([])
  const [loadingProdutos, setLoadingProdutos] = useState(true)
  const [loadingSebos, setLoadingSebos] = useState(true)
  const [errorProdutos, setErrorProdutos] = useState<string | null>(null)
  const [errorSebos, setErrorSebos] = useState<string | null>(null)
  const [totalProdutos, setTotalProdutos] = useState(0)
  const [totalSebos, setTotalSebos] = useState(0)
  const [totalPaginasProdutos, setTotalPaginasProdutos] = useState(0)
  const [totalPaginasSebos, setTotalPaginasSebos] = useState(0)

  useEffect(() => {
    // Keep the editable input aligned with browser navigation.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setInputQ(q)
    setCondicao(appliedCondicao)
    setSeboId(appliedSeboId)
    setPrecoMin(appliedPrecoMin)
    setPrecoMax(appliedPrecoMax)
    setCategoriaId(appliedCategoriaId)
    setBookGenre(appliedBookGenre)
    setCidade(appliedCidade)
    setUf(appliedUf)
  }, [q, appliedCondicao, appliedSeboId, appliedPrecoMin, appliedPrecoMax, appliedCategoriaId, appliedBookGenre, appliedCidade, appliedUf])

  useEffect(() => {
    categoriaService.listar().then((page) => setCategorias(getAllowedCategories(page.content))).catch(() => setCategorias([]))
  }, [])

  useEffect(() => {
    const params = {
      q: q || undefined,
      seboId: appliedSeboId ? Number(appliedSeboId) : undefined,
      condicao: appliedCondicao ? (appliedCondicao as CondicaoProduto) : undefined,
      precoMin: appliedPrecoMin ? Number(appliedPrecoMin) : undefined,
      precoMax: appliedPrecoMax ? Number(appliedPrecoMax) : undefined,
      categoriaId: appliedCategoriaId ? Number(appliedCategoriaId) : undefined,
      bookGenre: appliedBookGenre ? (appliedBookGenre as BookGenre) : undefined,
    }

    const request = currentPage === 0
      ? buscaService.buscarProdutos(params)
      : buscaService.buscarProdutos(params, currentPage)
    request
      .then((page) => {
        setErrorProdutos(null)
        setProdutos(page.content)
        setTotalProdutos(page.totalElements)
        setTotalPaginasProdutos(page.totalPages)
      })
      .catch((err) => {
        setErrorProdutos(extractErrorMessage(err, 'Nao foi possivel carregar os produtos.'))
        setProdutos([])
        setTotalProdutos(0)
        setTotalPaginasProdutos(0)
      })
      .finally(() => setLoadingProdutos(false))
  }, [q, appliedSeboId, appliedCondicao, appliedPrecoMin, appliedPrecoMax, appliedCategoriaId, appliedBookGenre, currentPage])

  useEffect(() => {
    const params = {
      q: q || undefined,
      cidade: appliedCidade || undefined,
      uf: appliedUf || undefined,
      lat: proximidadeAtiva && coords ? coords.lat : undefined,
      lng: proximidadeAtiva && coords ? coords.lng : undefined,
    }

    const request = currentPage === 0
      ? buscaService.buscarSebos(params)
      : buscaService.buscarSebos(params, currentPage)
    request
      .then((page) => {
        setErrorSebos(null)
        setSebos(page.content)
        setTotalSebos(page.totalElements)
        setTotalPaginasSebos(page.totalPages)
      })
      .catch((err) => {
        setErrorSebos(extractErrorMessage(err, 'Nao foi possivel carregar os sebos.'))
        setSebos([])
        setTotalSebos(0)
        setTotalPaginasSebos(0)
      })
      .finally(() => setLoadingSebos(false))
  }, [q, appliedCidade, appliedUf, currentPage, proximidadeAtiva, coords])

  function ativarProximidade() {
    if (!('geolocation' in navigator)) {
      setGeoError('Seu navegador nao suporta geolocalizacao.')
      return
    }
    setGeoLoading(true)
    setGeoError(null)
    navigator.geolocation.getCurrentPosition(
      (position) => {
        setCoords({ lat: position.coords.latitude, lng: position.coords.longitude })
        setProximidadeAtiva(true)
        setGeoLoading(false)
        setLoadingSebos(true)
        setErrorSebos(null)
        const next = new URLSearchParams(searchParams)
        next.delete('page')
        setSearchParams(next)
      },
      () => {
        setGeoError('Nao foi possivel obter sua localizacao. Verifique a permissao do navegador.')
        setGeoLoading(false)
      },
      { enableHighAccuracy: false, timeout: 10000, maximumAge: 5 * 60 * 1000 },
    )
  }

  function desativarProximidade() {
    setProximidadeAtiva(false)
    setGeoError(null)
    setLoadingSebos(true)
    setErrorSebos(null)
    const next = new URLSearchParams(searchParams)
    next.delete('page')
    setSearchParams(next)
  }

  function formatarDistancia(distanciaKm: number) {
    return distanciaKm < 1
      ? `${Math.round(distanciaKm * 1000)} m`
      : `${distanciaKm.toFixed(1)} km`
  }

  function handleSearch(e: React.FormEvent) {
    e.preventDefault()
    const next = new URLSearchParams(searchParams)
    if (inputQ.trim()) {
      next.set('q', inputQ.trim())
    } else {
      next.delete('q')
    }
    next.delete('page')
    if (next.toString() === searchParams.toString()) return
    setLoadingProdutos(true)
    setLoadingSebos(true)
    setErrorProdutos(null)
    setErrorSebos(null)
    setSearchParams(next)
  }

  function setTab(t: Tab) {
    const next = new URLSearchParams(searchParams)
    next.set('tab', t)
    next.delete('page')
    setSearchParams(next)
  }

  function aplicarFiltros(e: React.FormEvent) {
    e.preventDefault()
    const next = new URLSearchParams(searchParams)
    next.delete('page')
    if (tab === 'produtos') {
      if (condicao) next.set('condicao', condicao)
      else next.delete('condicao')
      if (seboId) next.set('seboId', seboId)
      else next.delete('seboId')
      if (precoMin) next.set('precoMin', precoMin)
      else next.delete('precoMin')
      if (precoMax) next.set('precoMax', precoMax)
      else next.delete('precoMax')
      if (categoriaId) next.set('categoriaId', categoriaId)
      else next.delete('categoriaId')
      if (bookGenre && isBookCategory(categorias.find((item) => item.id === Number(categoriaId)))) next.set('bookGenre', bookGenre)
      else next.delete('bookGenre')
      if (next.toString() === searchParams.toString()) return
      setLoadingProdutos(true)
      setErrorProdutos(null)
    } else {
      if (cidade.trim()) next.set('cidade', cidade.trim())
      else next.delete('cidade')
      if (uf.trim()) next.set('uf', uf.trim().toUpperCase())
      else next.delete('uf')
      if (next.toString() === searchParams.toString()) return
      setLoadingSebos(true)
      setErrorSebos(null)
    }
    setSearchParams(next)
  }

  function limparFiltros() {
    const next = new URLSearchParams()
    if (q) next.set('q', q)
    next.set('tab', tab)
    if (next.toString() === searchParams.toString()) return
    setSearchParams(next)
    setCondicao('')
    setSeboId('')
    setPrecoMin('')
    setPrecoMax('')
    setCategoriaId('')
    setBookGenre('')
    setCidade('')
    setUf('')
    setLoadingProdutos(true)
    setLoadingSebos(true)
    setErrorProdutos(null)
    setErrorSebos(null)
  }

  function mudarPagina(page: number) {
    const next = new URLSearchParams(searchParams)
    if (page <= 0) next.delete('page')
    else next.set('page', String(page + 1))
    if (tab === 'produtos') setLoadingProdutos(true)
    else setLoadingSebos(true)
    setSearchParams(next)
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  return (
    <div className="page busca-page">
      <main className="container busca-main">
        <form className="busca-search-bar" onSubmit={handleSearch}>
          <input
            type="search"
            aria-label="Pesquisar produtos ou sebos"
            placeholder="O que você está procurando?"
            value={inputQ}
            onChange={(e) => setInputQ(e.target.value)}
          />
          <button type="submit">Buscar</button>
        </form>

        {q ? <p className="busca-termo">Resultados para: <strong>"{q}"</strong></p> : null}

        <div className="busca-layout">
          <aside className="busca-filtros">
            <form onSubmit={aplicarFiltros}>
              <h3>Filtros</h3>
              {tab === 'produtos' ? (
                <>
                  <label className="busca-filtro-label">
                    Categoria
                    <select value={categoriaId} onChange={(e) => { setCategoriaId(e.target.value); setBookGenre('') }}>
                      <option value="">Todas</option>
                      {categorias.map((category) => <option key={category.id} value={category.id}>{category.nome}</option>)}
                    </select>
                  </label>
                  {isBookCategory(categorias.find((item) => item.id === Number(categoriaId))) ? (
                    <label className="busca-filtro-label">Gênero do livro
                      <select value={bookGenre} onChange={(e) => setBookGenre(e.target.value)}>
                        <option value="">Todos</option>
                        {BOOK_GENRE_OPTIONS.map((genre) => <option key={genre.value} value={genre.value}>{genre.label}</option>)}
                      </select>
                    </label>
                  ) : null}
                  <label className="busca-filtro-label">
                    Condição
                    <select value={condicao} onChange={(e) => setCondicao(e.target.value)}>
                      <option value="">Todas</option>
                      <option value={CondicaoProduto.NOVO}>Novo</option>
                      <option value={CondicaoProduto.SEMINOVO}>Seminovo</option>
                      <option value={CondicaoProduto.USADO}>Usado</option>
                    </select>
                  </label>
                  <label className="busca-filtro-label">
                    Código do sebo (opcional)
                    <input
                      type="number"
                      min={1}
                      value={seboId}
                      onChange={(e) => setSeboId(e.target.value)}
                      placeholder="ex: 1"
                    />
                  </label>
                  <label className="busca-filtro-label">
                    Preço mín. (R$)
                    <input
                      type="number"
                      min={0}
                      step={0.01}
                      value={precoMin}
                      onChange={(e) => setPrecoMin(e.target.value)}
                      placeholder="0,00"
                    />
                  </label>
                  <label className="busca-filtro-label">
                    Preço máx. (R$)
                    <input
                      type="number"
                      min={0}
                      step={0.01}
                      value={precoMax}
                      onChange={(e) => setPrecoMax(e.target.value)}
                      placeholder="sem limite"
                    />
                  </label>
                </>
              ) : (
                <>
                  <div className="busca-proximidade">
                    {!proximidadeAtiva ? (
                      <button
                        type="button"
                        className="busca-proximidade-btn"
                        onClick={ativarProximidade}
                        disabled={geoLoading}
                      >
                        {geoLoading ? 'Obtendo localização...' : '📍 Ordenar por proximidade'}
                      </button>
                    ) : (
                      <button
                        type="button"
                        className="busca-proximidade-btn busca-proximidade-btn-ativo"
                        onClick={desativarProximidade}
                      >
                        📍 Ordenado por proximidade — cancelar
                      </button>
                    )}
                    {geoError ? <p className="busca-proximidade-erro">{geoError}</p> : null}
                  </div>
                  <label className="busca-filtro-label">
                    Cidade
                    <input value={cidade} onChange={(e) => setCidade(e.target.value)} placeholder="ex: Fortaleza" />
                  </label>
                  <label className="busca-filtro-label">
                    UF
                    <input maxLength={2} value={uf} onChange={(e) => setUf(e.target.value.toUpperCase().slice(0, 2))} placeholder="ex: CE" />
                  </label>
                </>
              )}
              <button type="submit" className="busca-filtro-btn">Aplicar</button>
              <button type="button" className="busca-filtro-btn busca-filtro-btn-ghost" onClick={limparFiltros}>
                Limpar
              </button>
            </form>
          </aside>

          <section className="busca-resultados">
            <div className="busca-tabs" role="tablist" aria-label="Tipo de resultado">
              <button
                type="button"
                role="tab"
                aria-selected={tab === 'produtos'}
                className={`busca-tab${tab === 'produtos' ? ' active' : ''}`}
                onClick={() => setTab('produtos')}
              >
                Produtos {totalProdutos > 0 ? `(${totalProdutos})` : ''}
              </button>
              <button
                type="button"
                role="tab"
                aria-selected={tab === 'sebos'}
                className={`busca-tab${tab === 'sebos' ? ' active' : ''}`}
                onClick={() => setTab('sebos')}
              >
                Sebos {totalSebos > 0 ? `(${totalSebos})` : ''}
              </button>
            </div>

            {tab === 'produtos' ? (
              errorProdutos ? (
                <div className="flash error" role="alert">{errorProdutos}</div>
              ) : loadingProdutos ? (
                <div className="busca-grid">
                  {Array.from({ length: 8 }).map((_, i) => (
                    <div key={i} className="skeleton-card" />
                  ))}
                </div>
              ) : produtos.length === 0 ? (
                <p className="busca-empty">Nenhum produto encontrado.</p>
              ) : (
                <div className="busca-grid">
                  {produtos.map((p) => (
                    <ProductCard key={p.id} produto={p} />
                  ))}
                </div>
              )
            ) : errorSebos ? (
              <div className="flash error" role="alert">{errorSebos}</div>
            ) : loadingSebos ? (
              <p className="busca-empty">Carregando sebos...</p>
            ) : sebos.length === 0 ? (
              <p className="busca-empty">Nenhum sebo encontrado.</p>
            ) : (
              <div className="busca-sebos-lista">
                {sebos.map((s) => (
                  <Link key={s.id} className="busca-sebo-card" to={`/vendedor/${s.id}`}>
                    {s.fotoUrl ? (
                      <img src={s.fotoUrl} alt={s.nome} className="busca-sebo-foto" />
                    ) : (
                      <div className="busca-sebo-foto busca-sebo-foto-placeholder">S</div>
                    )}
                    <div className="busca-sebo-info">
                      <h3>{s.nome}</h3>
                      {s.descricao ? <p className="busca-sebo-desc">{s.descricao}</p> : null}
                      {s.cidade || s.uf ? <p className="busca-sebo-tel">{[s.cidade, s.uf].filter(Boolean).join(' - ')}</p> : null}
                      {s.telefone ? <p className="busca-sebo-tel">{s.telefone}</p> : null}
                      {proximidadeAtiva && s.distanciaKm != null ? (
                        <p className="busca-sebo-distancia">📍 {formatarDistancia(s.distanciaKm)} de você</p>
                      ) : null}
                    </div>
                  </Link>
                ))}
              </div>
            )}
            {tab === 'produtos' && !loadingProdutos && !errorProdutos ? (
              <Pagination
                page={currentPage}
                totalPages={totalPaginasProdutos}
                onPageChange={mudarPagina}
                label="Páginas de produtos"
              />
            ) : null}
            {tab === 'sebos' && !loadingSebos && !errorSebos ? (
              <Pagination
                page={currentPage}
                totalPages={totalPaginasSebos}
                onPageChange={mudarPagina}
                label="Páginas de sebos"
              />
            ) : null}
          </section>
        </div>
      </main>
    </div>
  )
}
