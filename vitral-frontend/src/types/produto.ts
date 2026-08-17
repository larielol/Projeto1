export const CondicaoProduto = {
  NOVO: 'NOVO',
  USADO: 'USADO',
  SEMINOVO: 'SEMINOVO',
} as const

export type CondicaoProduto = (typeof CondicaoProduto)[keyof typeof CondicaoProduto]

export type ProdutoRequest = {
  titulo: string
  categoriaId?: number
  bookGenre?: BookGenre
  classico?: boolean
  lancamento?: boolean
  autor?: string
  descricao?: string
  ano?: number
  preco: number
  estoque?: number
  condicao: CondicaoProduto
  fotoUrl?: string
}

export type SugestaoProduto = {
  titulo: string
  autor: string | null
  ano: number | null
  descricao: string | null
  categoriaId: number | null
  categoriaSlug: string | null
  categoriaNome: string | null
  fotoUrl: string | null
  fonte: string
}

export type Produto = {
  id: number
  seboId: number
  categoriaId: number | null
  categoriaNome: string | null
  bookGenre?: BookGenre | null
  classico?: boolean
  lancamento?: boolean
  titulo: string
  autor: string | null
  descricao: string | null
  ano?: number | null
  preco: number
  precoPromocional?: number | null
  estoque: number | null
  condicao: CondicaoProduto
  fotoUrl: string | null
  ativo: boolean
  disponivel?: boolean
  seboVerificado?: boolean
  seboNome?: string | null
  dataPublicacao?: string | null
  createdAt?: string | null
}

export type VendaFisicaResponse = {
  mensagem: string
  estoqueAtual?: number
}

export type ProdutoVendedor = {
  produtoId: number
  seboId: number
  seboNome: string
  preco: number
  precoPromocional: number | null
  precoEfetivo: number
  estoque: number
  condicao: CondicaoProduto
}
import type { BookGenre } from '../constants/productCatalog'
