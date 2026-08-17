import type { Categoria } from '../types/categoria'

export const ProductCategory = {
  LIVROS: 'LIVROS',
  CDS: 'CDS',
  VINIS: 'VINIS',
  HQS_MANGAS: 'HQS_MANGAS',
} as const

export type ProductCategory = (typeof ProductCategory)[keyof typeof ProductCategory]

export const BookGenre = {
  ROMANCE: 'ROMANCE', FICCAO: 'FICCAO', FANTASIA: 'FANTASIA', TERROR: 'TERROR',
  SUSPENSE: 'SUSPENSE', MISTERIO: 'MISTERIO', AVENTURA: 'AVENTURA', BIOGRAFIA: 'BIOGRAFIA',
  HISTORIA: 'HISTORIA', FILOSOFIA: 'FILOSOFIA', POESIA: 'POESIA', AUTOAJUDA: 'AUTOAJUDA',
  INFANTIL: 'INFANTIL', TECNICO: 'TECNICO', DIDATICO: 'DIDATICO', OUTROS: 'OUTROS',
} as const

export type BookGenre = (typeof BookGenre)[keyof typeof BookGenre]

export const BOOK_GENRE_OPTIONS: ReadonlyArray<{ value: BookGenre; label: string }> = [
  { value: BookGenre.ROMANCE, label: 'Romance' }, { value: BookGenre.FICCAO, label: 'Ficção' },
  { value: BookGenre.FANTASIA, label: 'Fantasia' }, { value: BookGenre.TERROR, label: 'Terror' },
  { value: BookGenre.SUSPENSE, label: 'Suspense' }, { value: BookGenre.MISTERIO, label: 'Mistério' },
  { value: BookGenre.AVENTURA, label: 'Aventura' }, { value: BookGenre.BIOGRAFIA, label: 'Biografia' },
  { value: BookGenre.HISTORIA, label: 'História' }, { value: BookGenre.FILOSOFIA, label: 'Filosofia' },
  { value: BookGenre.POESIA, label: 'Poesia' }, { value: BookGenre.AUTOAJUDA, label: 'Autoajuda' },
  { value: BookGenre.INFANTIL, label: 'Infantil' }, { value: BookGenre.TECNICO, label: 'Técnico' },
  { value: BookGenre.DIDATICO, label: 'Didático' }, { value: BookGenre.OUTROS, label: 'Outros' },
]

const normalize = (value?: string) => (value ?? '').normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase().replace(/[^a-z0-9]/g, '')

const CATEGORY_ALIASES: Record<string, ProductCategory> = {
  livro: ProductCategory.LIVROS, livros: ProductCategory.LIVROS,
  cd: ProductCategory.CDS, cds: ProductCategory.CDS,
  vinil: ProductCategory.VINIS, vinis: ProductCategory.VINIS,
  hq: ProductCategory.HQS_MANGAS, hqs: ProductCategory.HQS_MANGAS, manga: ProductCategory.HQS_MANGAS,
  mangas: ProductCategory.HQS_MANGAS, hqmanga: ProductCategory.HQS_MANGAS, hqsmangas: ProductCategory.HQS_MANGAS,
}

export function getCategoryType(category?: Pick<Categoria, 'nome' | 'slug'> | null): ProductCategory | null {
  if (!category) return null
  return CATEGORY_ALIASES[normalize(category.slug)] ?? CATEGORY_ALIASES[normalize(category.nome)] ?? null
}

export function getAllowedCategories(categories: Categoria[]): Categoria[] {
  return categories.filter((category) => getCategoryType(category) !== null)
}

export function isBookCategory(category?: Pick<Categoria, 'nome' | 'slug'> | null): boolean {
  return getCategoryType(category) === ProductCategory.LIVROS
}

export function bookGenreLabel(genre?: BookGenre | null): string | null {
  return BOOK_GENRE_OPTIONS.find((item) => item.value === genre)?.label ?? null
}
