export type Mensagem = {
  id: number
  remetenteId: number
  remetenteNome: string
  destinatarioId: number
  destinatarioNome: string
  conteudo: string
  lida: boolean
  createdAt: string
}

export type MensagemRequest = {
  destinatarioId: number
  conteudo: string
}
