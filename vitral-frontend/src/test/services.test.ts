import { describe, expect, it, vi } from 'vitest'
import { api } from '../services/api'
import { categoriaService } from '../services/categoriaService'
import { cestaService } from '../services/cestaService'
import { favoritoService } from '../services/favoritoService'
import { homeService } from '../services/homeService'
import { mensagemService } from '../services/mensagemService'
import { ofertaService } from '../services/ofertaService'
import { pedidoService } from '../services/pedidoService'
import { recomendacaoService } from '../services/recomendacaoService'
import { produtoService } from '../services/produtoService'
import { seboService } from '../services/seboService'
import { uploadService } from '../services/uploadService'
import { StatusPedido } from '../types/pedido'

const page = { content: [], totalElements: 0, totalPages: 0, number: 0, size: 20, first: true, last: true }

describe('contratos dos servicos com o backend', () => {
  it('lista recomendações com limite máximo e limpa o histórico', async () => {
    const get = vi.spyOn(api, 'get').mockResolvedValue({ data: page })
    const remove = vi.spyOn(api, 'delete').mockResolvedValue({ data: { mensagem: 'ok' } })

    await recomendacaoService.listar(2, 100)
    await recomendacaoService.limparHistorico()

    expect(get).toHaveBeenCalledWith('/recomendacoes', { params: { page: 2, size: 50 } })
    expect(remove).toHaveBeenCalledWith('/recomendacoes/historico')
  })

  it('carrega todas as seções da home pelo endpoint centralizado', async () => {
    const home = { lancamentos: null, classicos: null, recentes: null, recomendados: null, categorias: [] }
    const get = vi.spyOn(api, 'get').mockResolvedValue({ data: home })

    await expect(homeService.carregar()).resolves.toBe(home)

    expect(get).toHaveBeenCalledWith('/home')
  })

  it('busca detalhes reais do produto e perfil publico do sebo por ID', async () => {
    const produto = { id: 7, titulo: 'Livro' }
    const sebo = { id: 3, nome: 'Sebo Central' }
    const get = vi.spyOn(api, 'get')
      .mockResolvedValueOnce({ data: produto })
      .mockResolvedValueOnce({ data: sebo })

    await expect(produtoService.buscarPorId(7)).resolves.toBe(produto)
    await expect(seboService.buscarPorId(3)).resolves.toBe(sebo)
    expect(get).toHaveBeenNthCalledWith(1, '/produtos/7')
    expect(get).toHaveBeenNthCalledWith(2, '/sebos/3')
  })

  it('lista, adiciona e remove favoritos', async () => {
    const post = vi.spyOn(api, 'post').mockResolvedValue({ data: { mensagem: 'ok' } })
    const get = vi.spyOn(api, 'get').mockResolvedValue({ data: [] })
    const remove = vi.spyOn(api, 'delete').mockResolvedValue({ data: undefined })

    await favoritoService.favoritar(9)
    await favoritoService.listar()
    await favoritoService.remover(9)

    expect(post).toHaveBeenCalledWith('/favoritos/9')
    expect(get).toHaveBeenCalledWith('/favoritos')
    expect(remove).toHaveBeenCalledWith('/favoritos/9')
  })

  it('persiste cesta e atualiza quantidade', async () => {
    const post = vi.spyOn(api, 'post').mockResolvedValue({ data: { mensagem: 'ok' } })
    const put = vi.spyOn(api, 'put').mockResolvedValue({ data: { mensagem: 'ok' } })
    const get = vi.spyOn(api, 'get').mockResolvedValue({ data: [] })

    await cestaService.adicionar(4, 2)
    await cestaService.atualizarQuantidade(4, 3)
    await cestaService.listar()

    expect(post).toHaveBeenCalledWith('/cesta/4', null, { params: { quantidade: 2 } })
    expect(put).toHaveBeenCalledWith('/cesta/4', null, { params: { quantidade: 3 } })
    expect(get).toHaveBeenCalledWith('/cesta')
  })

  it('confirma, cancela e acompanha pedidos e vendas', async () => {
    const pedido = { id: 11, status: StatusPedido.AGUARDANDO_CONFIRMACAO }
    const post = vi.spyOn(api, 'post').mockResolvedValue({ data: pedido })
    const put = vi.spyOn(api, 'put').mockResolvedValue({ data: pedido })
    const get = vi.spyOn(api, 'get').mockResolvedValue({ data: page })

    await pedidoService.confirmarPedido({ formaPagamento: 'PIX' })
    await pedidoService.cancelarPedido(11)
    await pedidoService.atualizarStatus(11, StatusPedido.CONFIRMADO)
    await pedidoService.listarMeusPedidos()
    await pedidoService.listarVendas()

    expect(post).toHaveBeenCalledWith('/pedidos', { formaPagamento: 'PIX' })
    expect(put).toHaveBeenCalledWith('/pedidos/11/cancelar')
    expect(put).toHaveBeenCalledWith('/pedidos/11/status', null, {
      params: { status: StatusPedido.CONFIRMADO },
    })
    expect(get).toHaveBeenCalledWith('/pedidos/meus-pedidos', { params: { page: 0, size: 20 } })
    expect(get).toHaveBeenCalledWith('/pedidos/vendas', { params: { page: 0, size: 20 } })
  })

  it('consulta categorias, produtos por categoria, lancamentos e classicos', async () => {
    const get = vi.spyOn(api, 'get').mockResolvedValue({ data: page })

    await categoriaService.listar()
    await produtoService.listarPorCategoria(5)
    await produtoService.listarLancamentos()
    await produtoService.listarClassicos()

    expect(get).toHaveBeenCalledWith('/categorias', { params: { page: 0, size: 50 } })
    expect(get).toHaveBeenCalledWith('/produtos/categoria/5', { params: { page: 0, size: 20 } })
    expect(get).toHaveBeenCalledWith('/produtos/lancamentos', { params: { page: 0, size: 20 } })
    expect(get).toHaveBeenCalledWith('/produtos/classicos', { params: { page: 0, size: 20 } })
  })

  it('envia e lista mensagens e conversas', async () => {
    const post = vi.spyOn(api, 'post').mockResolvedValue({ data: { id: 1 } })
    const get = vi.spyOn(api, 'get').mockResolvedValue({ data: page })

    await mensagemService.enviar({ destinatarioId: 2, conteudo: 'Ola' })
    await mensagemService.listar()
    await mensagemService.listarConversa(2)

    expect(post).toHaveBeenCalledWith('/mensagens', { destinatarioId: 2, conteudo: 'Ola' })
    expect(get).toHaveBeenCalledWith('/mensagens', { params: { page: 0, size: 50 } })
    expect(get).toHaveBeenCalledWith('/mensagens/conversa/2', { params: { page: 0, size: 50 } })
  })

  it('lista e administra ofertas', async () => {
    const payload = { produtoId: 3, precoPromocional: 19.9 }
    const get = vi.spyOn(api, 'get').mockResolvedValue({ data: page })
    const post = vi.spyOn(api, 'post').mockResolvedValue({ data: payload })
    const put = vi.spyOn(api, 'put').mockResolvedValue({ data: payload })
    const remove = vi.spyOn(api, 'delete').mockResolvedValue({ data: undefined })

    await ofertaService.listarAtivas()
    await ofertaService.criar(payload)
    await ofertaService.atualizar(8, payload)
    await ofertaService.remover(8)

    expect(get).toHaveBeenCalledWith('/ofertas', { params: { page: 0, size: 20 } })
    expect(post).toHaveBeenCalledWith('/ofertas', payload)
    expect(put).toHaveBeenCalledWith('/ofertas/8', payload)
    expect(remove).toHaveBeenCalledWith('/ofertas/8')
  })

  it('envia imagem via multipart', async () => {
    const post = vi.spyOn(api, 'post').mockResolvedValue({ data: { url: '/api/v1/uploads/images/foto.jpg' } })
    const file = new File(['foto'], 'foto.jpg', { type: 'image/jpeg' })

    await expect(uploadService.uploadImage(file)).resolves.toEqual({ url: '/api/v1/uploads/images/foto.jpg' })
    expect(post).toHaveBeenCalledWith('/uploads/images', expect.any(FormData), {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
  })
})
