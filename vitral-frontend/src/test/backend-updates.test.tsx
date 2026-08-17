import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'
import { MeuSeboPage } from '../pages/MeuSeboPage'
import { pedidoService } from '../services/pedidoService'
import { seboService } from '../services/seboService'
import { useAuthStore } from '../store/authStore'
import { AccountType } from '../types/account'

const account = { id: 1, name: 'Sebo Virtual', username: 'sebo.virtual', email: 'sebo@vitral.com', type: AccountType.SEBO }
const sebo = { id: 3, accountId: 1, nome: 'Sebo Virtual', email: account.email, descricao: 'Online', telefone: '9999', fotoUrl: null }
function authenticateSebo() {
  useAuthStore.setState({ token: 'jwt', account, seboId: 3 })
}

describe('mudancas recentes do backend', () => {
  it('exibe faturamento anual com os doze meses e permite trocar o ano', async () => {
    authenticateSebo()
    vi.spyOn(seboService, 'buscarPorId').mockResolvedValue(sebo)
    const billing = vi.spyOn(pedidoService, 'buscarFaturamentoMensal')
      .mockResolvedValueOnce([{ ano: 2026, mes: 1, vendasOnline: 1200, reembolsos: 200, total: 1000 }])
      .mockResolvedValueOnce([])

    render(<MemoryRouter><MeuSeboPage /></MemoryRouter>)

    expect((await screen.findAllByText(/R\$\s*1\.000,00/)).length).toBeGreaterThan(0)
    expect(screen.getAllByText(/R\$\s*1\.200,00/).length).toBeGreaterThan(0)
    expect(screen.getAllByText(/R\$\s*200,00/).length).toBeGreaterThan(0)
    expect(screen.getAllByRole('row')).toHaveLength(13)
    await userEvent.selectOptions(screen.getByLabelText('Ano'), '2025')
    await waitFor(() => expect(billing).toHaveBeenLastCalledWith(2025))
    expect(await screen.findByText('Nenhum faturamento registrado neste ano.')).toBeInTheDocument()
  })

})
