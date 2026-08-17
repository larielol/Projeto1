import { api } from './api'
import type { Page } from '../types/common'
import type { ConfirmarPedidoRequest, FaturamentoMensal, Pedido, StatusPedido } from '../types/pedido'

export const pedidoService = {
  async confirmarPedido(payload: ConfirmarPedidoRequest): Promise<Pedido> {
    const { data } = await api.post<Pedido>('/pedidos', payload)
    return data
  },

  async atualizarStatus(id: number, status: StatusPedido): Promise<Pedido> {
    const { data } = await api.put<Pedido>(`/pedidos/${id}/status`, null, {
      params: { status },
    })
    return data
  },

  async cancelarPedido(id: number): Promise<Pedido> {
    const { data } = await api.put<Pedido>(`/pedidos/${id}/cancelar`)
    return data
  },

  async reembolsar(id: number): Promise<Pedido> {
    const { data } = await api.put<Pedido>(`/pedidos/${id}/reembolsar`)
    return data
  },

  async listarMeusPedidos(page = 0, size = 20, status?: StatusPedido): Promise<Page<Pedido>> {
    const { data } = await api.get<Page<Pedido>>('/pedidos/meus-pedidos', {
      params: { page, size, status },
    })
    return data
  },

  async listarVendas(page = 0, size = 20, status?: StatusPedido): Promise<Page<Pedido>> {
    const { data } = await api.get<Page<Pedido>>('/pedidos/vendas', {
      params: { page, size, status },
    })
    return data
  },

  async buscarFaturamentoMensal(ano: number): Promise<FaturamentoMensal[]> {
    const { data } = await api.get<FaturamentoMensal[]>('/pedidos/faturamento-mensal', {
      params: { ano },
    })
    return data
  },
}
