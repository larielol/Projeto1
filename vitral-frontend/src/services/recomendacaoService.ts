import { api } from './api'
import type { Page } from '../types/common'
import type { Produto } from '../types/produto'
import type { MensagemResponse } from '../types/account'

const MAX_PAGE_SIZE = 50

export const recomendacaoService = {
  async listar(page = 0, size = 20): Promise<Page<Produto>> {
    const safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE)
    const { data } = await api.get<Page<Produto>>('/recomendacoes', {
      params: { page: Math.max(page, 0), size: safeSize },
    })
    return data
  },

  async limparHistorico(): Promise<MensagemResponse> {
    const { data } = await api.delete<MensagemResponse>('/recomendacoes/historico')
    return data
  },
}
