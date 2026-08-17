import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { SeboDocumentsPage } from '../pages/SeboDocumentsPage'
import { seboService } from '../services/seboService'
import { TOKEN_KEY } from '../services/api'
import { useAuthStore } from '../store/authStore'
import { AccountType } from '../types/account'
import { StatusVerificacao, TipoDocumentoSebo } from '../types/sebo'
import { abrirDocumentoAutenticado } from '../utils/documentoAutenticado'

const documento = {
  id: 1,
  tipo: TipoDocumentoSebo.CARTAO_CNPJ,
  arquivoUrl: '/api/v1/uploads/documents/arquivo.pdf',
  nomeArquivo: 'arquivo.pdf',
  contentType: 'application/pdf',
  tamanhoBytes: 2048,
  enviadoEm: '2026-07-06T10:00:00Z',
  status: StatusVerificacao.PENDENTE,
  motivoRejeicao: null,
}

describe('abertura autenticada de documentos de verificacao', () => {
  beforeEach(() => {
    vi.restoreAllMocks()
    if (!URL.createObjectURL) {
      URL.createObjectURL = vi.fn()
    }
  })

  it('envia o Bearer token ao buscar o documento, ao inves de navegar direto para a URL protegida', async () => {
    localStorage.setItem(TOKEN_KEY, 'token-de-teste')
    const blob = new Blob(['conteudo'], { type: 'application/pdf' })
    const fetchSpy = vi.spyOn(globalThis, 'fetch').mockResolvedValue({
      ok: true,
      blob: async () => blob,
    } as Response)
    const objectUrlSpy = vi.spyOn(URL, 'createObjectURL').mockReturnValue('blob:mock-url')
    const janelaFalsa: { location: { href: string }, close: () => void, opener?: unknown } = {
      location: { href: '' }, close: vi.fn(),
    }
    const openSpy = vi.spyOn(window, 'open').mockReturnValue(janelaFalsa as unknown as Window)

    await abrirDocumentoAutenticado('/api/v1/uploads/documents/arquivo.pdf')

    // A flag "noopener" faz window.open() retornar null em navegadores reais, o que
    // quebrava a abertura do documento (aba ficava presa em about:blank para sempre).
    // Essa asseracao existe para nunca deixarem reintroduzir a flag aqui.
    expect(openSpy).toHaveBeenNthCalledWith(1, '', '_blank')
    expect(janelaFalsa.opener).toBeNull()
    expect(fetchSpy).toHaveBeenCalledWith('/api/v1/uploads/documents/arquivo.pdf', {
      headers: { Authorization: 'Bearer token-de-teste' },
    })
    expect(objectUrlSpy).toHaveBeenCalledWith(blob)
    expect(janelaFalsa.location.href).toBe('blob:mock-url')
  })

  it('fecha a aba aberta e propaga o erro quando o backend recusa o acesso (403)', async () => {
    localStorage.setItem(TOKEN_KEY, 'token-de-teste')
    vi.spyOn(globalThis, 'fetch').mockResolvedValue({ ok: false, status: 403 } as Response)
    const janelaFalsa = { location: { href: '' }, close: vi.fn() }
    vi.spyOn(window, 'open').mockReturnValue(janelaFalsa as unknown as Window)

    await expect(abrirDocumentoAutenticado('/api/v1/uploads/documents/arquivo.pdf')).rejects.toThrow()

    expect(janelaFalsa.close).toHaveBeenCalled()
  })

  it('mostra uma mensagem de erro na tela quando o documento nao pode ser aberto', async () => {
    const seboAccount = { id: 1, name: 'Sebo', username: 'sebo', email: 'sebo@teste.com', type: AccountType.SEBO }
    useAuthStore.setState({ token: 'jwt', account: seboAccount, seboId: 3 })
    vi.spyOn(seboService, 'listarMeusDocumentos').mockResolvedValue([documento])
    vi.spyOn(window, 'open').mockReturnValue(null)
    vi.spyOn(globalThis, 'fetch').mockRejectedValue(new Error('offline'))

    render(<MemoryRouter><SeboDocumentsPage /></MemoryRouter>)

    await userEvent.click(await screen.findByRole('button', { name: 'Abrir' }))

    expect(await screen.findByText('Não foi possível abrir o documento.')).toBeInTheDocument()
  })
})
