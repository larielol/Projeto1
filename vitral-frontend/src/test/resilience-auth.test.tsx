import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { ErrorBoundary } from '../components/ErrorBoundary'
import { ResendConfirmationPage } from '../pages/ResendConfirmationPage'
import { authService } from '../services/authService'

function BrokenComponent(): never {
  throw new Error('falha de renderização')
}

describe('resiliência e confirmação de conta', () => {
  afterEach(() => vi.restoreAllMocks())

  it('exibe uma recuperação amigável quando a interface falha', () => {
    vi.spyOn(console, 'error').mockImplementation(() => {})
    render(
      <ErrorBoundary>
        <BrokenComponent />
      </ErrorBoundary>,
    )

    expect(screen.getByRole('heading', { name: 'Algo não saiu como esperado' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Tentar novamente' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Voltar ao início' })).toHaveAttribute('href', '/')
  })

  it('reenvia a confirmação usando o e-mail informado', async () => {
    const resend = vi.spyOn(authService, 'reenviarConfirmacao').mockResolvedValue({
      mensagem: 'Se a conta estiver pendente, enviaremos um novo link de confirmação.',
    })
    render(
      <MemoryRouter initialEntries={['/auth/reenviar-confirmacao?email=conta%40vitral.test']}>
        <ResendConfirmationPage />
      </MemoryRouter>,
    )

    expect(screen.getByLabelText('E-mail:')).toHaveValue('conta@vitral.test')
    await userEvent.click(screen.getByRole('button', { name: 'Reenviar link' }))

    expect(resend).toHaveBeenCalledWith({ email: 'conta@vitral.test' })
    expect(await screen.findByRole('status')).toHaveTextContent('novo link de confirmação')
  })
})
