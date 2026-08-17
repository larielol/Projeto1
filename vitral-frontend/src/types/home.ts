import type { Produto } from './produto'

export type HomeSection = {
  titulo: string
  produtos: Produto[]
  total: number
}

export type HomeCategorySection = {
  id: number
  nome: string
  slug: string
  produtos: Produto[]
  total: number
}

export type HomeResponse = {
  lancamentos: HomeSection
  classicos: HomeSection
  recentes: HomeSection
  recomendados: HomeSection | null
  categorias: HomeCategorySection[]
}
