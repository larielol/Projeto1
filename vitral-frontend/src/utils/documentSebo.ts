import type { TipoDocumentoSebo } from '../types/sebo'

export const documentLabels: Record<TipoDocumentoSebo, string> = {
  CARTAO_CNPJ: 'Cartão CNPJ',
  CONTRATO_SOCIAL: 'Contrato social',
  DOCUMENTO_RESPONSAVEL: 'Documento do responsável',
  COMPROVANTE_BANCARIO: 'Comprovante bancário',
  COMPROVANTE_ATIVIDADE: 'Comprovante de atividade',
  OUTRO: 'Outro',
}

export function formatBytes(value?: number | null) {
  if (!value) return '—'
  if (value < 1024) return `${value} B`
  if (value < 1024 * 1024) return `${(value / 1024).toFixed(1)} KB`
  return `${(value / 1024 / 1024).toFixed(1)} MB`
}
