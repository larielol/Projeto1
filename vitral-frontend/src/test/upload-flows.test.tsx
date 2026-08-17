import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { MeuSeboPage } from '../pages/MeuSeboPage'
import { MeusProdutosPage } from '../pages/MeusProdutosPage'
import { PerfilPage } from '../pages/PerfilPage'
import { categoriaService } from '../services/categoriaService'
import { produtoService } from '../services/produtoService'
import { seboService } from '../services/seboService'
import { uploadService } from '../services/uploadService'
import { usuarioService } from '../services/usuarioService'
import { useAuthStore } from '../store/authStore'
import { AccountType } from '../types/account'
import { CondicaoProduto } from '../types/produto'

const emptyPage = {
  content: [],
  totalElements: 0,
  totalPages: 0,
  number: 0,
  size: 20,
  first: true,
  last: true,
}

const uploadedUrl = '/api/v1/uploads/images/foto-teste.png'

function renderPage(component: React.ReactNode) {
  return render(<MemoryRouter>{component}</MemoryRouter>)
}

describe('fluxos de upload de imagem', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
    vi.spyOn(uploadService, 'uploadImage').mockResolvedValue({ url: uploadedUrl })
  })

  it('salva no perfil a URL retornada pelo upload', async () => {
    const account = {
      id: 1,
      name: 'Rute',
      username: 'rute',
      email: 'rute@teste.com',
      type: AccountType.USUARIO,
      fotoUrl: null,
      cpf: '52998224725', cep: '60000000', logradouro: 'Rua A', numero: '1', complemento: null, bairro: 'Centro', cidade: 'Fortaleza', estado: 'CE',
    }
    useAuthStore.setState({ token: 'jwt', account, seboId: null })
    vi.spyOn(usuarioService, 'buscarPerfil').mockResolvedValue(account)
    vi.spyOn(usuarioService, 'atualizarPerfil').mockResolvedValue({ ...account, fotoUrl: uploadedUrl })

    renderPage(<PerfilPage />)
    const file = new File(['imagem'], 'perfil.png', { type: 'image/png' })
    await userEvent.upload(screen.getByLabelText('Foto do perfil'), file)
    await userEvent.click(screen.getByRole('button', { name: 'Salvar alterações' }))

    expect(uploadService.uploadImage).toHaveBeenCalledWith(file)
    await waitFor(() => expect(usuarioService.atualizarPerfil).toHaveBeenCalledWith({
      name: 'Rute', fotoUrl: uploadedUrl,
    }))
    expect(useAuthStore.getState().account?.fotoUrl).toBe(uploadedUrl)
  })

  it('inclui a URL enviada ao cadastrar o sebo', async () => {
    const account = { id: 2, name: 'Sebo', username: 'sebo', email: 'sebo@teste.com', type: AccountType.SEBO }
    const savedSebo = {
      id: 3,
      accountId: 2,
      nome: 'Sebo',
      email: 'sebo@teste.com',
      descricao: null,
      telefone: null,
      cep: null,
      logradouro: null,
      cidade: null,
      uf: null,
      horarioFuncionamento: null,
      fotoUrl: uploadedUrl,
    }
    useAuthStore.setState({ token: 'jwt', account, seboId: null })
    vi.spyOn(seboService, 'buscarMeu').mockRejectedValue(new Error('Sebo nao encontrado'))
    vi.spyOn(seboService, 'criar').mockResolvedValue(savedSebo)

    renderPage(<MeuSeboPage />)
    expect(await screen.findByText('Cadastrar perfil do sebo')).toBeInTheDocument()
    const file = new File(['imagem'], 'sebo.webp', { type: 'image/webp' })
    await userEvent.upload(screen.getByLabelText('Foto do sebo'), file)
    await userEvent.type(screen.getByLabelText('CNPJ'), '12345678000199')
    await userEvent.type(screen.getByLabelText('CEP'), '60123456')
    await userEvent.type(screen.getByLabelText('Endereço/logradouro'), 'Rua dos Livros')
    await userEvent.type(screen.getByLabelText('Cidade'), 'Fortaleza')
    await userEvent.type(screen.getByLabelText('UF'), 'CE')
    await userEvent.click(screen.getByRole('button', { name: 'Cadastrar' }))

    await waitFor(() => expect(seboService.criar).toHaveBeenCalledWith(
      expect.objectContaining({ fotoUrl: uploadedUrl }),
    ))
  })

  it('inclui a URL enviada ao cadastrar um produto', async () => {
    const account = { id: 2, name: 'Sebo', username: 'sebo', email: 'sebo@teste.com', type: AccountType.SEBO }
    useAuthStore.setState({ token: 'jwt', account, seboId: 3 })
    vi.spyOn(categoriaService, 'listar').mockResolvedValue({ ...emptyPage, content: [{ id: 2, nome: 'CDs', slug: 'cds', descricao: null }] })
    vi.spyOn(produtoService, 'listarPorSebo').mockResolvedValue(emptyPage)
    vi.spyOn(produtoService, 'criar').mockResolvedValue({
      id: 7,
      seboId: 3,
      categoriaId: null,
      categoriaNome: null,
      titulo: 'Livro com foto',
      autor: null,
      descricao: null,
      preco: 25,
      estoque: 1,
      condicao: CondicaoProduto.USADO,
      fotoUrl: uploadedUrl,
      ativo: true,
    })

    renderPage(<MeusProdutosPage />)
    expect(await screen.findByText('Nenhum produto cadastrado')).toBeInTheDocument()
    await userEvent.type(screen.getByLabelText('Título'), 'Livro com foto')
    await userEvent.clear(screen.getByLabelText('Preço (R$)'))
    await userEvent.type(screen.getByLabelText('Preço (R$)'), '25')
    const file = new File(['imagem'], 'produto.jpg', { type: 'image/jpeg' })
    await userEvent.upload(screen.getByLabelText('Foto do produto'), file)
    await userEvent.selectOptions(screen.getByLabelText('Categoria'), '2')
    await userEvent.click(screen.getByRole('button', { name: 'Adicionar produto' }))

    await waitFor(() => expect(produtoService.criar).toHaveBeenCalledWith(
      expect.objectContaining({ titulo: 'Livro com foto', fotoUrl: uploadedUrl }),
    ))
  })
})
