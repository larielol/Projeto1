import { api } from './api'
import type { MensagemResponse } from '../types/account'
import type { Favorito } from '../types/favorito'

export const favoritoService = {
  async favoritar(produtoId: number): Promise<MensagemResponse> {
    const { data } = await api.post<MensagemResponse>(`/favoritos/${produtoId}`)
    return data
  },

  async listar(): Promise<Favorito[]> {
    const { data } = await api.get<Favorito[]>('/favoritos')
    return data
  },

  async remover(produtoId: number): Promise<void> {
    await api.delete(`/favoritos/${produtoId}`)
  },
}
