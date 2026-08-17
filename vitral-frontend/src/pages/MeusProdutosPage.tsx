import { useCallback, useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { StoreStatus } from '../components/Layout/StorePage'
import { Button } from '../components/ui/Button'
import { CurrencyInput } from '../components/ui/CurrencyInput'
import { ImageUploadField } from '../components/ui/ImageUploadField'
import { Input } from '../components/ui/Input'
import { Pagination } from '../components/ui/Pagination'
import { categoriaService } from '../services/categoriaService'
import { produtoService } from '../services/produtoService'
import { extractErrorMessage } from '../services/api'
import { seboService } from '../services/seboService'
import { useAuth } from '../hooks/useAuth'
import type { Categoria } from '../types/categoria'
import { StatusVerificacao, type StatusVerificacao as VerificationStatus } from '../types/sebo'
import { CondicaoProduto } from '../types/produto'
import type { Produto, ProdutoRequest } from '../types/produto'
import './FormPage.css'
import './MeusProdutosPage.css'
import { BOOK_GENRE_OPTIONS, bookGenreLabel, getAllowedCategories, isBookCategory, type BookGenre } from '../constants/productCatalog'
import type { SugestaoProduto } from '../types/produto'

const EMPTY: ProdutoRequest = {
  titulo: '',
  autor: '',
  descricao: '',
  ano: undefined,
  preco: 0,
  estoque: 1,
  condicao: CondicaoProduto.USADO,
  classico: false,
  fotoUrl: '',
}

const CATEGORIAS_MUSICAIS = ['cds', 'vinis']

export function MeusProdutosPage() {
  const { seboId } = useAuth()
  const [produtos, setProdutos] = useState<Produto[]>([])
  const [categorias, setCategorias] = useState<Categoria[]>([])
  const [loading, setLoading] = useState(true)
  const [form, setForm] = useState<ProdutoRequest>(EMPTY)
  const [editingId, setEditingId] = useState<number | null>(null)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [totalProducts, setTotalProducts] = useState(0)
  const [verification, setVerification] = useState<VerificationStatus | null>(null)
  const [categoryFilter, setCategoryFilter] = useState('')
  const [genreFilter, setGenreFilter] = useState('')
  const [sugestoes, setSugestoes] = useState<SugestaoProduto[]>([])
  const [buscandoSugestoes, setBuscandoSugestoes] = useState(false)
  const [sugestaoAviso, setSugestaoAviso] = useState<string | null>(null)

  const allowedCategories = useMemo(() => getAllowedCategories(categorias), [categorias])
  const selectedCategory = allowedCategories.find((item) => item.id === form.categoriaId)
  const bookSelected = isBookCategory(selectedCategory)
  const autorLabel = CATEGORIAS_MUSICAIS.includes(selectedCategory?.slug ?? '') ? 'Artista' : 'Autor'
  const visibleProducts = produtos.filter((item) => {
    if (categoryFilter && item.categoriaId !== Number(categoryFilter)) return false
    if (genreFilter && item.bookGenre !== genreFilter) return false
    return true
  })

  const load = useCallback(() => {
    if (!seboId) {
      setLoading(false)
      return
    }
    setLoading(true)
    produtoService
      .listarPorSebo(seboId, page, 10)
      .then((result) => {
        setProdutos(result.content)
        setTotalPages(result.totalPages)
        setTotalProducts(result.totalElements)
      })
      .catch((err) => {
        setProdutos([])
        setError(extractErrorMessage(err, 'Erro ao listar produtos'))
      })
      .finally(() => setLoading(false))
  }, [page, seboId])

  useEffect(() => {
    // Reload the catalog when the authenticated sebo changes.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    load()
  }, [load])

  useEffect(() => {
    let active = true
    categoriaService
      .listar()
      .then((page) => {
        if (active) setCategorias(page.content)
      })
      .catch(() => {
        if (active) {
          setCategorias([])
          setError('Não foi possível carregar as categorias permitidas.')
        }
      })

    return () => {
      active = false
    }
  }, [])

  useEffect(() => {
    if (!seboId) return
    seboService.buscarPorId(seboId).then((item) => setVerification(item.statusVerificacao ?? null)).catch(() => setVerification(null))
  }, [seboId])

  function update<K extends keyof ProdutoRequest>(key: K, value: ProdutoRequest[K]) {
    setForm((prev) => ({ ...prev, [key]: value }))
  }

  async function handleBuscarSugestoes() {
    const termo = form.titulo.trim()
    setSugestoes([])
    if (termo.length < 3) {
      setSugestaoAviso('Digite ao menos 3 caracteres do título para buscar.')
      return
    }
    setBuscandoSugestoes(true)
    setSugestaoAviso(null)
    try {
      const encontradas = await produtoService.buscarSugestoes(termo)
      setSugestoes(encontradas)
      if (encontradas.length === 0) {
        setSugestaoAviso('Nenhuma informação encontrada. Preencha os campos manualmente.')
      }
    } catch (err) {
      setSugestaoAviso(extractErrorMessage(err, 'Não foi possível buscar as informações agora.'))
    } finally {
      setBuscandoSugestoes(false)
    }
  }

  function aplicarSugestao(sugestao: SugestaoProduto) {
    setForm((current) => ({
      ...current,
      titulo: sugestao.titulo,
      autor: sugestao.autor ?? current.autor,
      ano: sugestao.ano ?? current.ano,
      descricao: sugestao.descricao ?? current.descricao,
      categoriaId: sugestao.categoriaId ?? current.categoriaId,
      bookGenre: sugestao.categoriaId && sugestao.categoriaId !== current.categoriaId ? undefined : current.bookGenre,
      fotoUrl: current.fotoUrl ? current.fotoUrl : sugestao.fotoUrl ?? '',
    }))
    setSugestoes([])
    setSugestaoAviso(null)
  }

  function startEdit(p: Produto) {
    setEditingId(p.id)
    setForm({
      titulo: p.titulo,
      categoriaId: p.categoriaId ?? undefined,
      bookGenre: p.bookGenre ?? undefined,
      classico: p.classico ?? false,
      autor: p.autor ?? '',
      descricao: p.descricao ?? '',
      ano: p.ano ?? undefined,
      preco: p.preco,
      estoque: p.estoque ?? 1,
      condicao: p.condicao,
      fotoUrl: p.fotoUrl ?? '',
    })
    setError(null)
    setSuccess(null)
  }

  function resetForm() {
    setEditingId(null)
    setForm(EMPTY)
    setSugestoes([])
    setSugestaoAviso(null)
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setError(null)
    setSuccess(null)
    setBusy(true)
    try {
      if (!form.categoriaId || !allowedCategories.some((category) => category.id === form.categoriaId)) {
        setError('Selecione uma das quatro categorias permitidas.')
        return
      }
      const payload: ProdutoRequest = {
        ...form,
        bookGenre: bookSelected ? form.bookGenre : undefined,
        ano: form.ano ? Number(form.ano) : undefined,
        preco: Number(form.preco),
        estoque: Number(form.estoque ?? 1),
      }
      if (editingId) {
        await produtoService.atualizar(editingId, payload)
        setSuccess('Produto atualizado!')
      } else {
        await produtoService.criar(payload)
        setSuccess('Produto cadastrado!')
      }
      resetForm()
      load()
    } catch (err) {
      setError(extractErrorMessage(err, 'Erro ao salvar produto'))
    } finally {
      setBusy(false)
    }
  }

  async function handleRemove(p: Produto) {
    if (!confirm(`Remover "${p.titulo}"?`)) return
    try {
      await produtoService.remover(p.id)
      load()
    } catch (err) {
      setError(extractErrorMessage(err, 'Erro ao remover produto'))
    }
  }

  if (!seboId) {
    return (
      <div className="page">
        <main className="container form-page">
          <h1 className="form-page-title">Cadastre seu sebo primeiro</h1>
          <StoreStatus
            title="Perfil do sebo necessário"
            description="Você precisa criar o perfil do seu sebo antes de adicionar produtos ao catálogo."
            action={<Link to="/painel/sebo">Cadastrar sebo</Link>}
          />
        </main>
      </div>
    )
  }

  if (verification && verification !== StatusVerificacao.VERIFICADO) {
    return <div className="page"><main className="container form-page"><StoreStatus title="Sebo ainda não verificado" description="Cadastro de produtos e vendas serão liberados após a aprovação dos documentos." tone={verification === StatusVerificacao.REJEITADO ? 'error' : 'neutral'} action={<Link to="/painel/sebo/documentos">Gerenciar documentos</Link>} /></main></div>
  }

  return (
    <div className="page">
      <main className="container form-page">
        <h1 className="form-page-title">Meus produtos</h1>
        {error ? <div className="flash error" role="alert">{error}</div> : null}
        {success ? <div className="flash success" role="status">{success}</div> : null}

        <form className="form-grid" onSubmit={handleSubmit}>
          <div className="titulo-com-busca">
            <Input id="titulo" label="Título" required
              value={form.titulo} onChange={(e) => update('titulo', e.target.value)} />
            <button type="button" className="sugestao-trigger" onClick={handleBuscarSugestoes}
              disabled={buscandoSugestoes}>
              {buscandoSugestoes ? 'Buscando informações...' : 'Buscar informações pelo título'}
            </button>
          </div>
          <Input id="autor" label={autorLabel}
            value={form.autor ?? ''} onChange={(e) => update('autor', e.target.value)} />
          {sugestaoAviso || sugestoes.length > 0 ? (
            <div className="produto-sugestoes">
              {sugestaoAviso ? <p className="produto-sugestoes-aviso" role="status">{sugestaoAviso}</p> : null}
              {sugestoes.length > 0 ? (
                <>
                  <p className="produto-sugestoes-titulo">Selecione o resultado correto para preencher o cadastro:</p>
                  <ul className="produto-sugestoes-lista">
                    {sugestoes.map((sugestao, indice) => (
                      <li key={`${sugestao.fonte}-${sugestao.titulo}-${indice}`}>
                        <button type="button" className="produto-sugestao" onClick={() => aplicarSugestao(sugestao)}>
                          <strong>{sugestao.titulo}</strong>
                          <span>
                            {[sugestao.autor, sugestao.ano, sugestao.categoriaNome].filter(Boolean).join(' • ')}
                          </span>
                        </button>
                      </li>
                    ))}
                  </ul>
                </>
              ) : null}
            </div>
          ) : null}
          <CurrencyInput id="preco" label="Preço (R$)" required
            value={form.preco} onChange={(value) => update('preco', value)} />
          <div className="campo-duplo">
            <Input id="estoque" label="Estoque" type="number" step="1" min="1"
              value={form.estoque ?? 1} onChange={(e) => update('estoque', Number(e.target.value))} />
            <Input id="ano" label="Ano" type="number" step="1" min="0" max="2200" placeholder="Ex: 1969"
              value={form.ano ?? ''} onChange={(e) => update('ano', e.target.value ? Number(e.target.value) : undefined)} />
          </div>
          <label className="vit-input-wrapper">
            <span className="vit-input-label">Categoria</span>
            <select
              className="vit-input"
              value={form.categoriaId ?? ''}
              required
              onChange={(e) => setForm((current) => ({ ...current, categoriaId: e.target.value ? Number(e.target.value) : undefined, bookGenre: undefined }))}
            >
              <option value="">Selecione</option>
              {allowedCategories.map((categoria) => (
                <option key={categoria.id} value={categoria.id}>{categoria.nome}</option>
              ))}
            </select>
          </label>
          {bookSelected ? (
            <label className="vit-input-wrapper">
              <span className="vit-input-label">Gênero do livro</span>
              <select className="vit-input" value={form.bookGenre ?? ''} onChange={(e) => update('bookGenre', e.target.value as BookGenre)}>
                <option value="">Selecione</option>
                {BOOK_GENRE_OPTIONS.map((genre) => <option key={genre.value} value={genre.value}>{genre.label}</option>)}
              </select>
            </label>
          ) : null}
          <label className="vit-input-wrapper">
            <span className="vit-input-label">Condição</span>
            <select
              className="vit-input"
              value={form.condicao}
              onChange={(e) => update('condicao', e.target.value as CondicaoProduto)}
            >
              <option value={CondicaoProduto.NOVO}>Novo</option>
              <option value={CondicaoProduto.SEMINOVO}>Seminovo</option>
              <option value={CondicaoProduto.USADO}>Usado</option>
            </select>
          </label>
          <label className="produto-checkbox">
            <input type="checkbox" checked={form.classico ?? false} onChange={(e) => update('classico', e.target.checked)} />
            <span>Destacar como clássico</span>
          </label>
          <ImageUploadField
            id="fotoUrl"
            label="Foto do produto"
            value={form.fotoUrl}
            onChange={(url) => update('fotoUrl', url)}
          />
          <Input id="descricao" label="Descrição"
            value={form.descricao ?? ''} onChange={(e) => update('descricao', e.target.value)} />
          <div className="form-actions">
            {editingId ? (
              <Button type="button" variant="ghost" onClick={resetForm}>Cancelar</Button>
            ) : null}
            <Button type="submit" disabled={busy}>
              {busy ? 'Salvando...' : editingId ? 'Atualizar produto' : 'Adicionar produto'}
            </Button>
          </div>
        </form>

        <h2 className="section-title">Catálogo ({totalProducts})</h2>
        <div className="produto-filters" aria-label="Filtros do catálogo">
          <label className="vit-input-wrapper"><span className="vit-input-label">Filtrar por categoria</span><select className="vit-input" value={categoryFilter} onChange={(e) => { setCategoryFilter(e.target.value); setGenreFilter('') }}><option value="">Todas</option>{allowedCategories.map((category) => <option key={category.id} value={category.id}>{category.nome}</option>)}</select></label>
          {isBookCategory(allowedCategories.find((item) => item.id === Number(categoryFilter))) ? <label className="vit-input-wrapper"><span className="vit-input-label">Filtrar por gênero</span><select className="vit-input" value={genreFilter} onChange={(e) => setGenreFilter(e.target.value)}><option value="">Todos</option>{BOOK_GENRE_OPTIONS.map((genre) => <option key={genre.value} value={genre.value}>{genre.label}</option>)}</select></label> : null}
        </div>
        {loading ? (
          <StoreStatus
            title="Carregando catálogo"
            description="Buscando os produtos do seu catálogo."
            busy
          />
        ) : visibleProducts.length === 0 ? (
          <StoreStatus
            title="Nenhum produto cadastrado"
            description="Adicione o primeiro item usando o formulário acima para começar seu catálogo."
          />
        ) : (
          <>
            <div className="produto-table-scroll">
              <table className="produto-table">
              <thead>
                <tr>
                  <th scope="col">Título</th>
                  <th scope="col">Autor</th>
                  <th scope="col">Categoria</th>
                  <th scope="col">Gênero</th>
                  <th scope="col">Preço</th>
                  <th scope="col">Estoque</th>
                  <th scope="col">Condição</th>
                  <th scope="col">Ações</th>
                </tr>
              </thead>
              <tbody>
                {visibleProducts.map((p) => (
                  <tr key={p.id}>
                    <td>{p.titulo}</td>
                    <td>{p.autor ?? '-'}</td>
                    <td>{p.categoriaNome ?? '-'}</td>
                    <td>{bookGenreLabel(p.bookGenre) ?? '-'}</td>
                    <td>R$ {Number(p.preco).toFixed(2)}</td>
                    <td>{p.estoque ?? 0}</td>
                    <td>{p.condicao}</td>
                    <td className="produto-table-actions">
                      <Button type="button" variant="ghost" onClick={() => startEdit(p)}>Editar</Button>
                      <Button type="button" variant="danger" onClick={() => handleRemove(p)}>Remover</Button>
                    </td>
                  </tr>
                ))}
              </tbody>
              </table>
            </div>
            <Pagination
              page={page}
              totalPages={totalPages}
              onPageChange={setPage}
              label="Páginas do catálogo"
            />
          </>
        )}
      </main>
    </div>
  )
}
