import { api } from './api'

export interface SupportePayload {
  assunto: string
  mensagem: string
}

export async function enviarSuporte(payload: SupportePayload): Promise<void> {
  await api.post('/suporte', payload)
}
