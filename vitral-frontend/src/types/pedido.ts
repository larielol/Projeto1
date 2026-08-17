export const StatusPedido = {
  AGUARDANDO_CONFIRMACAO: 'AGUARDANDO_CONFIRMACAO',
  CONFIRMADO: 'CONFIRMADO',
  CANCELADO: 'CANCELADO',
  REEMBOLSADO: 'REEMBOLSADO',
} as const

export type StatusPedido = (typeof StatusPedido)[keyof typeof StatusPedido]

export const FormaPagamento = {
  CARTAO: 'CARTAO',
  PIX: 'PIX',
  BOLETO: 'BOLETO',
} as const

export type FormaPagamento = (typeof FormaPagamento)[keyof typeof FormaPagamento]

export const StatusPagamento = {
  PENDENTE: 'PENDENTE',
  APROVADO: 'APROVADO',
  RECUSADO: 'RECUSADO',
} as const

export type StatusPagamento = (typeof StatusPagamento)[keyof typeof StatusPagamento]

export type ConfirmarPedidoRequest = {
  formaPagamento: FormaPagamento
  numeroCartao?: string
}

export type PedidoItem = {
  id: number
  produtoId: number
  tituloSnapshot: string
  precoSnapshot: number
  quantidade: number
}

export type Pedido = {
  id: number
  accountId: number
  seboId: number
  status: StatusPedido
  formaPagamento?: FormaPagamento | null
  statusPagamento?: StatusPagamento
  total: number
  createdAt: string
  confirmadoEm?: string | null
  pagoEm?: string | null
  canceladoEm?: string | null
  reembolsadoEm?: string | null
  itens: PedidoItem[]
}

export type FaturamentoMensal = {
  ano: number
  mes: number
  vendasOnline: number
  reembolsos: number
  total: number
}
