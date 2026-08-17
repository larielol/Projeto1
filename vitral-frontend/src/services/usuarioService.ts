import { api } from './api'
import type { Account, UpdateProfileRequest } from '../types/account'
import type { MensagemResponse } from '../types/account'

export const usuarioService = {
  async buscarPerfil(): Promise<Account> {
    const { data } = await api.get<Account>('/auth/me')
    return data
  },

  async atualizarPerfil(payload: UpdateProfileRequest): Promise<Account> {
    const { data } = await api.put<Account>('/usuarios/me', payload)
    return data
  },

  async excluirConta(): Promise<MensagemResponse> {
    const { data } = await api.delete<MensagemResponse>('/usuarios/me')
    return data
  },
}
