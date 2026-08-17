import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { GlobalNavigation } from '../components/GlobalNavigation'
import { ProductCarousel } from '../components/ProductCarousel'
import { ProductPage } from '../components/Product/ProductPage'
import { SellerPage } from '../components/Seller/SellerPage'
import { MessagesPage } from '../components/Messages/MessagesPage'
import { ReleasesPage } from '../components/Releases/ReleasesPage'
import { ProductPlaceholder, StorePage, StoreStatus } from '../components/Layout/StorePage'
import { cestaService } from '../services/cestaService'
import { favoritoService } from '../services/favoritoService'
import { mensagemService } from '../services/mensagemService'
import { produtoService } from '../services/produtoService'
import { seboService } from '../services/seboService'
import { useAuthStore } from '../store/authStore'
import { AccountType } from '../types/account'
import { CondicaoProduto } from '../types/produto'

const emptyPage = { content: [], totalElements: 0, totalPages: 0, number: 0, size: 20, first: true, last: true }
const produto = { id: 7, seboId: 3, categoriaId: null, categoriaNome: null, titulo: 'Livro', autor: null, descricao: null, preco: 25, estoque: 2, condicao: CondicaoProduto.USADO, fotoUrl: 'https://img.test/livro.jpg', ativo: true }
const sebo = { id: 3, accountId: 8, nome: 'Sebo Central', email: 'sebo@teste.com', descricao: null, telefone: null, fotoUrl: null }

describe('ramos alternativos e interacoes', () => {
  beforeEach(() => useAuthStore.setState({ token: null, account: null, seboId: null }))

  it('trata produto com id invalido e produto inexistente', async () => {
    const buscar = vi.spyOn(produtoService, 'buscarPorId')
    const invalid = renderRoute('/produto/invalido', '/produto/:productId', <ProductPage />)
    expect(screen.getByText('Produto invalido')).toBeInTheDocument()
    expect(buscar).not.toHaveBeenCalled()
    invalid.unmount()
    buscar.mockRejectedValue(new Error('404'))
    renderRoute('/produto/99', '/produto/:productId', <ProductPage />)
    expect(await screen.findByText('Produto nao encontrado')).toBeInTheDocument()
  })

  it('mostra acoes corretas para visitante e sebo autenticado', async () => {
    vi.spyOn(produtoService, 'buscarPorId').mockResolvedValue(produto)
    vi.spyOn(produtoService, 'listarVendedores').mockResolvedValue([{ produtoId: 7, seboId: 3, seboNome: 'Sebo', preco: 25, precoPromocional: null, precoEfetivo: 25, estoque: 2, condicao: CondicaoProduto.USADO }])
    vi.spyOn(seboService, 'buscarPorId').mockResolvedValue(sebo)
    const guest = renderRoute('/produto/7', '/produto/:productId', <ProductPage />)
    expect(await screen.findByRole('link', { name: 'Entrar para comprar' })).toBeInTheDocument()
    expect(screen.getByRole('img', { name: 'Livro' })).toBeInTheDocument()
    guest.unmount()
    authenticate(AccountType.SEBO)
    renderRoute('/produto/7', '/produto/:productId', <ProductPage />)
    expect(await screen.findByRole('button', { name: 'Ações disponíveis para usuários' })).toBeDisabled()
  })

  it('exibe falhas ao adicionar na cesta e favoritar', async () => {
    authenticate(AccountType.USUARIO)
    vi.spyOn(produtoService, 'buscarPorId').mockResolvedValue(produto)
    vi.spyOn(produtoService, 'listarVendedores').mockResolvedValue([{ produtoId: 7, seboId: 3, seboNome: 'Sebo', preco: 25, precoPromocional: null, precoEfetivo: 25, estoque: 2, condicao: CondicaoProduto.USADO }])
    vi.spyOn(seboService, 'buscarPorId').mockRejectedValue(new Error('sem sebo'))
    vi.spyOn(cestaService, 'adicionar').mockRejectedValue(new Error('lotado'))
    vi.spyOn(favoritoService, 'favoritar').mockRejectedValue(new Error('falha'))
    renderRoute('/produto/7', '/produto/:productId', <ProductPage />)
    await userEvent.click(await screen.findByRole('button', { name: 'Adicionar à cesta' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('Nao foi possivel adicionar à cesta.')
    await userEvent.click(screen.getByRole('button', { name: 'Favoritar' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('Nao foi possivel favoritar o produto.')
    expect(screen.getAllByText('Sebo').length).toBeGreaterThan(0)
  })

  it('trata vendedor invalido, erro e catalogo vazio', async () => {
    const invalid = renderRoute('/vendedor/x', '/vendedor/:sellerId', <SellerPage />)
    expect(screen.getByText('ID de sebo invalido')).toBeInTheDocument()
    invalid.unmount()
    vi.spyOn(seboService, 'buscarPorId').mockRejectedValueOnce(new Error('offline')).mockResolvedValueOnce(sebo)
    vi.spyOn(produtoService, 'listarPorSebo').mockResolvedValue(emptyPage)
    const failed = renderRoute('/vendedor/3', '/vendedor/:sellerId', <SellerPage />)
    expect(await screen.findByText('Nao foi possivel carregar este sebo')).toBeInTheDocument()
    failed.unmount()
    renderRoute('/vendedor/3', '/vendedor/:sellerId', <SellerPage />)
    expect(await screen.findByText('Nenhum produto cadastrado')).toBeInTheDocument()
    expect(screen.queryByText('sebo@teste.com')).not.toBeInTheDocument()
    expect(screen.queryByText('Contato')).not.toBeInTheDocument()
  })

  it('seleciona conversa existente e trata falhas de conversa e envio', async () => {
    authenticate(AccountType.USUARIO)
    const received = { id: 1, remetenteId: 2, remetenteNome: 'Sebo', destinatarioId: 1, destinatarioNome: 'Rute', conteudo: 'Olá', lida: false, createdAt: '2026-06-24T10:00:00Z' }
    vi.spyOn(mensagemService, 'listar').mockResolvedValue({ ...emptyPage, content: [received] })
    vi.spyOn(mensagemService, 'listarConversa').mockRejectedValue(new Error('offline'))
    vi.spyOn(mensagemService, 'enviar').mockRejectedValue(new Error('falha'))
    renderPage(<MessagesPage />)
    await userEvent.click(await screen.findByRole('button', { name: /Sebo/ }))
    expect(await screen.findByRole('alert')).toHaveTextContent('Nao foi possivel carregar a conversa.')
    await userEvent.type(screen.getByRole('textbox', { name: 'Mensagem' }), 'Teste')
    await userEvent.click(screen.getByRole('button', { name: 'Enviar' }))
    expect(await screen.findByRole('alert')).toHaveTextContent('Nao foi possivel enviar a mensagem.')
  })

  it('renderiza lancamentos preenchidos e erro', async () => {
    vi.spyOn(produtoService, 'listarLancamentos').mockResolvedValueOnce({ ...emptyPage, content: [produto] }).mockRejectedValueOnce(new Error('offline'))
    const success = renderPage(<ReleasesPage />)
    expect(await screen.findByRole('link', { name: /Livro/ })).toBeInTheDocument()
    success.unmount()
    renderPage(<ReleasesPage />)
    expect(await screen.findByText('Erro ao carregar lançamentos')).toBeInTheDocument()
  })

  it('pesquisa, abre menu, fecha menu e encerra sessao pelo cabecalho', async () => {
    authenticate(AccountType.USUARIO)
    const logout = vi.fn()
    useAuthStore.setState({ logout })
    renderRoute('/', '*', <><GlobalNavigation /><Routes><Route path="/busca" element={<p>Busca aberta</p>} /></Routes></>)
    await userEvent.type(screen.getByLabelText('Pesquisar produtos'), ' livro raro ')
    await userEvent.click(screen.getByRole('button', { name: 'Buscar' }))
    expect(await screen.findByText('Busca aberta')).toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Abrir menu' }))
    expect(screen.getByRole('complementary', { name: 'Menu principal' })).toBeInTheDocument()
    await userEvent.click(screen.getByRole('link', { name: 'Categorias' }))
    expect(screen.queryByRole('complementary', { name: 'Menu principal' })).not.toBeInTheDocument()
    await userEvent.click(screen.getByRole('button', { name: 'Sair' }))
    expect(logout).toHaveBeenCalled()
  })

  it('exercita busca, status, placeholder e navegacao do carrossel', async () => {
    const scrollBy = vi.fn()
    Object.defineProperty(HTMLElement.prototype, 'scrollBy', { configurable: true, value: scrollBy })
    renderRoute('/', '*', <>
      <StorePage title="Teste" subtitle="Sub"><ProductPlaceholder label="Item" badge="Novo" /><StoreStatus title="Pronto" description="Ok" tone="success" /></StorePage>
      <ProductCarousel title="Vitrine" produtos={Array.from({ length: 6 }, (_, index) => ({ ...produto, id: index + 1 }))} />
    </>)
    await userEvent.type(screen.getByRole('textbox', { name: 'Pesquisar' }), 'romance')
    await userEvent.click(screen.getByRole('button', { name: 'Pesquisar' }))
    await userEvent.click(screen.getByRole('button', { name: 'Próximo' }))
    await userEvent.click(screen.getByRole('button', { name: 'Anterior' }))
    expect(scrollBy).toHaveBeenCalledTimes(2)
    expect(screen.getByText('Novo')).toBeInTheDocument()
  })
})

function authenticate(type: AccountType) {
  useAuthStore.setState({ token: 'jwt', account: { id: 1, name: 'Rute', username: 'rute', email: 'r@t.com', type }, seboId: type === AccountType.SEBO ? 3 : null })
}

function renderPage(element: React.ReactNode) {
  return render(<MemoryRouter>{element}</MemoryRouter>)
}

function renderRoute(path: string, pattern: string, element: React.ReactNode) {
  return render(<MemoryRouter initialEntries={[path]}><Routes><Route path={pattern} element={element} /></Routes></MemoryRouter>)
}
