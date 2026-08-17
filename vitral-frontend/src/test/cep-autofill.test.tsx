import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { MeuSeboPage } from '../pages/MeuSeboPage'
import { PerfilPage } from '../pages/PerfilPage'
import { pedidoService } from '../services/pedidoService'
import { seboService } from '../services/seboService'
import { usuarioService } from '../services/usuarioService'
import { useAuthStore } from '../store/authStore'
import { AccountType } from '../types/account'

const RESPOSTA_VIACEP = {
  cep: '58400-550',
  logradouro: 'Rua Rodrigues Alves',
  bairro: 'Prata',
  localidade: 'Campina Grande',
  uf: 'PB',
}

function mockFetchOk(corpo: unknown) {
  return vi.spyOn(globalThis, 'fetch').mockResolvedValue({
    ok: true,
    json: async () => corpo,
  } as Response)
}

describe('autopreenchimento de endereco pelo CEP (ViaCEP)', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
  })

  it('preenche logradouro, bairro, cidade e UF no perfil do usuario ao completar o CEP', async () => {
    const account = {
      id: 1, name: 'Rute', username: 'rute', email: 'rute@teste.com', type: AccountType.USUARIO,
      cep: null, logradouro: null, numero: null, complemento: null, bairro: null, cidade: null, estado: null,
    }
    useAuthStore.setState({ token: 'jwt', account, seboId: null })
    vi.spyOn(usuarioService, 'buscarPerfil').mockResolvedValue(account)
    const fetchSpy = mockFetchOk(RESPOSTA_VIACEP)

    render(<MemoryRouter><PerfilPage /></MemoryRouter>)

    await userEvent.type(await screen.findByLabelText('CEP'), '58400550')

    await waitFor(() => expect(fetchSpy).toHaveBeenCalledWith('https://viacep.com.br/ws/58400550/json/', expect.anything()))
    await waitFor(() => expect(screen.getByLabelText('Logradouro')).toHaveValue('Rua Rodrigues Alves'))
    expect(screen.getByLabelText('Bairro')).toHaveValue('Prata')
    expect(screen.getByLabelText('Cidade')).toHaveValue('Campina Grande')
    expect(screen.getByLabelText('Estado (UF)')).toHaveValue('PB')
  })

  it('mostra aviso e mantem os campos editaveis quando o CEP nao existe', async () => {
    const account = {
      id: 1, name: 'Rute', username: 'rute', email: 'rute@teste.com', type: AccountType.USUARIO,
      cep: null, logradouro: null, numero: null, complemento: null, bairro: null, cidade: null, estado: null,
    }
    useAuthStore.setState({ token: 'jwt', account, seboId: null })
    vi.spyOn(usuarioService, 'buscarPerfil').mockResolvedValue(account)
    mockFetchOk({ erro: true })

    render(<MemoryRouter><PerfilPage /></MemoryRouter>)

    await userEvent.type(await screen.findByLabelText('CEP'), '00000000')

    expect(await screen.findByText('CEP não encontrado. Preencha o endereço manualmente.')).toBeInTheDocument()
    expect(screen.getByLabelText('Logradouro')).toHaveValue('')
  })

  it('nao busca novamente quando o CEP carregado da conta permanece igual', async () => {
    const account = {
      id: 1, name: 'Rute', username: 'rute', email: 'rute@teste.com', type: AccountType.USUARIO,
      cep: '58400550', logradouro: 'Rua Antiga', numero: '10', complemento: null, bairro: 'Bairro Antigo',
      cidade: 'Cidade Antiga', estado: 'PB',
    }
    useAuthStore.setState({ token: 'jwt', account, seboId: null })
    vi.spyOn(usuarioService, 'buscarPerfil').mockResolvedValue(account)
    const fetchSpy = mockFetchOk(RESPOSTA_VIACEP)

    render(<MemoryRouter><PerfilPage /></MemoryRouter>)

    expect(await screen.findByLabelText('CEP')).toHaveValue('58400-550')
    expect(screen.getByLabelText('Logradouro')).toHaveValue('Rua Antiga')
    expect(fetchSpy).not.toHaveBeenCalled()
  })

  it('nao trava em "Buscando..." e libera o preenchimento manual quando a rede falha', async () => {
    const account = {
      id: 1, name: 'Rute', username: 'rute', email: 'rute@teste.com', type: AccountType.USUARIO,
      cep: null, logradouro: null, numero: null, complemento: null, bairro: null, cidade: null, estado: null,
    }
    useAuthStore.setState({ token: 'jwt', account, seboId: null })
    vi.spyOn(usuarioService, 'buscarPerfil').mockResolvedValue(account)
    vi.spyOn(globalThis, 'fetch').mockRejectedValue(new DOMException('The operation was aborted.', 'TimeoutError'))

    render(<MemoryRouter><PerfilPage /></MemoryRouter>)

    await userEvent.type(await screen.findByLabelText('CEP'), '58400550')

    expect(await screen.findByText('CEP não encontrado. Preencha o endereço manualmente.')).toBeInTheDocument()
    await userEvent.type(screen.getByLabelText('Logradouro'), 'Preenchido manualmente')
    expect(screen.getByLabelText('Logradouro')).toHaveValue('Preenchido manualmente')
  })

  it('preenche o endereco do sebo ao completar o CEP no cadastro', async () => {
    const seboAccount = { id: 2, name: 'Sebo', username: 'sebo', email: 'sebo@teste.com', type: AccountType.SEBO }
    useAuthStore.setState({ token: 'jwt', account: seboAccount, seboId: null })
    vi.spyOn(seboService, 'buscarMeu').mockRejectedValue(new Error('Sebo nao encontrado'))
    vi.spyOn(pedidoService, 'buscarFaturamentoMensal').mockResolvedValue([])
    const fetchSpy = mockFetchOk(RESPOSTA_VIACEP)

    render(<MemoryRouter><MeuSeboPage /></MemoryRouter>)
    expect(await screen.findByText('Cadastrar perfil do sebo')).toBeInTheDocument()

    await userEvent.type(screen.getByLabelText('CEP'), '58400550')

    await waitFor(() => expect(fetchSpy).toHaveBeenCalledWith('https://viacep.com.br/ws/58400550/json/', expect.anything()))
    await waitFor(() => expect(screen.getByLabelText('Endereço/logradouro')).toHaveValue('Rua Rodrigues Alves'))
    expect(screen.getByLabelText('Cidade')).toHaveValue('Campina Grande')
    expect(screen.getByLabelText('UF')).toHaveValue('PB')
  })
})
