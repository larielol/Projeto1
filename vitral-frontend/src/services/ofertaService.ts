import { api } from './api'
import type { Page } from '../types/common'
import type { Oferta, OfertaRequest } from '../types/oferta'

export const ofertaService = {
  async listarAtivas(page = 0, size = 20): Promise<Page<Oferta>> {
    const { data } = await api.get<Page<Oferta>>('/ofertas', {
      params: { page, size },
    })
    return data
  },

  async listarMinhas(page = 0, size = 20): Promise<Page<Oferta>> {
    const { data } = await api.get<Page<Oferta>>('/ofertas/minhas', {
      params: { page, size },
    })
    return data
  },

  async criar(payload: OfertaRequest): Promise<Oferta> {
    const { data } = await api.post<Oferta>('/ofertas', payload)
    return data
  },

  async atualizar(id: number, payload: OfertaRequest): Promise<Oferta> {
    const { data } = await api.put<Oferta>(`/ofertas/${id}`, payload)
    return data
  },

  async remover(id: number): Promise<void> {
    await api.delete(`/ofertas/${id}`)
  },
}
