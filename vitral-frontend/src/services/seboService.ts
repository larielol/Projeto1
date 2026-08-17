import { api } from './api'
import type { MensagemResponse } from '../types/account'
import type { AuditoriaSebo, ConsultaCnpjResponse, DocumentoSebo, Sebo, SeboRequest, StatusVerificacao, TipoDocumentoSebo } from '../types/sebo'
import type { Page } from '../types/common'

export const seboService = {
  async criar(payload: SeboRequest): Promise<Sebo> {
    const { data } = await api.post<Sebo>('/sebos', payload)
    return data
  },

  async atualizarMeu(payload: SeboRequest): Promise<Sebo> {
    const { data } = await api.put<Sebo>('/sebos/me', payload)
    return data
  },

  async buscarPorId(id: number): Promise<Sebo> {
    const { data } = await api.get<Sebo>(`/sebos/${id}`)
    return data
  },

  async buscarMeu(): Promise<Sebo> {
    const { data } = await api.get<Sebo>('/sebos/me')
    return data
  },

  async consultarMeuCnpj(): Promise<ConsultaCnpjResponse> {
    const { data } = await api.post<ConsultaCnpjResponse>('/sebos/me/verificacao/consultar-cnpj')
    return data
  },

  async listarPendentes(page = 0, size = 20): Promise<Page<Sebo>> {
    const { data } = await api.get<Page<Sebo>>('/sebos/verificacao/pendentes', {
      params: { page, size },
    })
    return data
  },

  async atualizarVerificacao(id: number, status: StatusVerificacao, motivo?: string): Promise<Sebo> {
    const body = motivo ? { motivo } : undefined
    const { data } = await api.put<Sebo>(`/sebos/${id}/verificacao`, body, {
      params: { status },
    })
    return data
  },

  async enviarDocumento(tipo: TipoDocumentoSebo, arquivo: File): Promise<DocumentoSebo> {
    const formData = new FormData()
    formData.append('tipo', tipo)
    formData.append('arquivo', arquivo)
    const { data } = await api.post<DocumentoSebo>('/sebos/me/documentos', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
    return data
  },
  async listarMeusDocumentos(): Promise<DocumentoSebo[]> {
    const { data } = await api.get<DocumentoSebo[]>('/sebos/me/documentos')
    return data
  },
  async listarDocumentos(id: number): Promise<DocumentoSebo[]> {
    const { data } = await api.get<DocumentoSebo[]>(`/sebos/${id}/documentos`)
    return data
  },
  async listarAuditoria(id: number): Promise<AuditoriaSebo[]> {
    const { data } = await api.get<AuditoriaSebo[]>(`/sebos/${id}/verificacao/auditoria`)
    return data
  },

  async excluirConta(): Promise<MensagemResponse> {
    const { data } = await api.delete<MensagemResponse>('/sebos/me')
    return data
  },
}
