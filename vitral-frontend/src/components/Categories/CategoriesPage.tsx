import { useEffect, useMemo, useState } from 'react'
import { useSearchParams } from 'react-router-dom'
import { ProductCard } from '../ProductCard'
import { StorePage, StoreStatus } from '../Layout/StorePage'
import { extractErrorMessage } from '../../services/api'
import { categoriaService } from '../../services/categoriaService'
import { produtoService } from '../../services/produtoService'
import type { Categoria } from '../../types/categoria'
import type { Produto } from '../../types/produto'
import './CategoriesPage.css'
import { BOOK_GENRE_OPTIONS, getAllowedCategories, isBookCategory, type BookGenre } from '../../constants/productCatalog'

export function CategoriesPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const selectedParam = searchParams.get('categoriaId')
  const selectedId = Number(selectedParam)
  const hasSelectedCategory = selectedParam !== null && Number.isFinite(selectedId)

  const [query, setQuery] = useState('')
  const [categories, setCategories] = useState<Categoria[]>([])
  const [products, setProducts] = useState<Produto[]>([])
  const [loadingCategories, setLoadingCategories] = useState(true)
  const [loadingProducts, setLoadingProducts] = useState(hasSelectedCategory)
  const [error, setError] = useState<string | null>(null)
  const [productError, setProductError] = useState<string | null>(null)
  const selectedGenre = (searchParams.get('bookGenre') || '') as BookGenre | ''

  const selectedCategory = categories.find((category) => category.id === selectedId)

  const filteredCategories = useMemo(() => {
    const normalizedQuery = query.trim().toLowerCase()
    if (!normalizedQuery) return categories
    return categories.filter((category) => category.nome.toLowerCase().includes(normalizedQuery))
  }, [categories, query])

  useEffect(() => {
    let active = true
    categoriaService
      .listar()
      .then((page) => {
        if (!active) return
        setCategories(getAllowedCategories(page.content))
        setError(null)
      })
      .catch((err) => {
        if (active) setError(extractErrorMessage(err, 'Nao foi possivel carregar as categorias.'))
      })
      .finally(() => {
        if (active) setLoadingCategories(false)
      })

    return () => {
      active = false
    }
  }, [])

  useEffect(() => {
    let active = true
    if (!hasSelectedCategory) {
      return
    }

    produtoService
      .listarPorCategoria(selectedId, 0, 20, selectedGenre || undefined)
      .then((page) => {
        if (!active) return
        setProducts(page.content)
      })
      .catch((err) => {
        if (active) setProductError(extractErrorMessage(err, 'Nao foi possivel carregar os produtos da categoria.'))
      })
      .finally(() => {
        if (active) setLoadingProducts(false)
      })

    return () => {
      active = false
    }
  }, [hasSelectedCategory, selectedId, selectedGenre])

  function selectCategory(category: Categoria) {
    setLoadingProducts(true)
    setProductError(null)
    setSearchParams({ categoriaId: String(category.id) })
  }

  function selectGenre(value: string) {
    setLoadingProducts(true)
    setProductError(null)
    const next = new URLSearchParams(searchParams)
    if (value) next.set('bookGenre', value)
    else next.delete('bookGenre')
    setSearchParams(next)
  }

  return (
    <StorePage
      title="Categorias"
      subtitle="Explore o acervo do Vitral por tipo de item."
      searchPlaceholder="Pesquisar uma categoria"
    >
      <form className="category-search" role="search" onSubmit={(event) => event.preventDefault()}>
        <input
          aria-label="Pesquisar uma categoria"
          placeholder="Pesquisar uma categoria"
          value={query}
          onChange={(event) => setQuery(event.target.value)}
        />
      </form>

      {loadingCategories ? (
        <StoreStatus title="Carregando categorias" description="Preparando as categorias do acervo." busy />
      ) : error ? (
        <StoreStatus title="Erro ao carregar categorias" description={error} tone="error" />
      ) : filteredCategories.length === 0 ? (
        <StoreStatus title="Nenhuma categoria encontrada" description="Tente buscar por outro termo." />
      ) : (
        <>
          <section className="category-grid" aria-label="Categorias do acervo">
            {filteredCategories.map((category, index) => (
              <button
                className={`category-card${category.id === selectedId ? ' is-selected' : ''}`}
                key={category.id}
                type="button"
                onClick={() => selectCategory(category)}
              >
                <span className="category-number">{String(index + 1).padStart(2, '0')}</span>
                <h2>{category.nome}</h2>
                {category.descricao ? <small>{category.descricao}</small> : null}
                <span className="category-arrow" aria-hidden="true">
                  →
                </span>
              </button>
            ))}
          </section>

          {hasSelectedCategory ? (
            <section className="category-products" aria-label="Produtos da categoria">
              <h2 className="section-title">
                {selectedCategory ? `Produtos em ${selectedCategory.nome}` : 'Produtos da categoria'}
              </h2>
              {isBookCategory(selectedCategory) ? (
                <label className="category-genre-filter">Gênero do livro
                  <select value={selectedGenre} onChange={(event) => selectGenre(event.target.value)}>
                    <option value="">Todos</option>
                    {BOOK_GENRE_OPTIONS.map((genre) => <option key={genre.value} value={genre.value}>{genre.label}</option>)}
                  </select>
                </label>
              ) : null}
              {loadingProducts ? (
                <StoreStatus title="Carregando produtos" description="Buscando produtos desta categoria." busy />
              ) : productError ? (
                <StoreStatus title="Erro ao carregar produtos" description={productError} tone="error" />
              ) : products.length === 0 ? (
                <StoreStatus
                  title="Nenhum produto nesta categoria"
                  description="Quando os sebos publicarem itens nessa categoria, eles aparecerão aqui."
                />
              ) : (
                <div className="busca-grid">
                  {products.map((product) => (
                    <ProductCard key={product.id} produto={product} />
                  ))}
                </div>
              )}
            </section>
          ) : null}
        </>
      )}
    </StorePage>
  )
}
