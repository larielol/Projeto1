import { api } from './api'
import type { Page } from '../types/common'
import type { Mensagem, MensagemRequest } from '../types/mensagem'

export const mensagemService = {
  async enviar(payload: MensagemRequest): Promise<Mensagem> {
    const { data } = await api.post<Mensagem>('/mensagens', payload)
    return data
  },

  async listar(page = 0, size = 50): Promise<Page<Mensagem>> {
    const { data } = await api.get<Page<Mensagem>>('/mensagens', {
      params: { page, size },
    })
    return data
  },

  async listarConversa(accountId: number, page = 0, size = 50): Promise<Page<Mensagem>> {
    const { data } = await api.get<Page<Mensagem>>(`/mensagens/conversa/${accountId}`, {
      params: { page, size },
    })
    return data
  },
}
