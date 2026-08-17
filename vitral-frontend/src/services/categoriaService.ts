import { api } from './api'
import type { Page } from '../types/common'
import type { Categoria } from '../types/categoria'

export const categoriaService = {
  async listar(page = 0, size = 50): Promise<Page<Categoria>> {
    const { data } = await api.get<Page<Categoria>>('/categorias', {
      params: { page, size },
    })
    return data
  },

  async criar(payload: { nome: string; descricao?: string }): Promise<Categoria> {
    const { data } = await api.post<Categoria>('/categorias', payload)
    return data
  },
}
