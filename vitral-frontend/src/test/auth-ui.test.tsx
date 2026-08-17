import { render, screen } from '@testing-library/react'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import { SideMenu } from '../components/SideMenu'
import { ProtectedRoute } from '../routes/ProtectedRoute'
import { useAuthStore } from '../store/authStore'
import { AccountType, type Account } from '../types/account'

describe('rotas e menu por perfil', () => {
  beforeEach(() => {
    useAuthStore.setState({ token: null, account: null, seboId: null })
  })

  it('redireciona visitante de rota protegida para autenticacao', () => {
    renderRoute(null, '/favoritos')
    expect(screen.getByText('Autenticacao')).toBeInTheDocument()
  })

  it('redireciona usuario sem perfil SEBO para a pagina inicial', () => {
    renderRoute(account(AccountType.USUARIO), '/vendas', AccountType.SEBO)
    expect(screen.getByText('Inicio')).toBeInTheDocument()
  })

  it('mostra opcoes de usuario e esconde vendas para conta USUARIO', () => {
    authenticate(account(AccountType.USUARIO))
    render(<MemoryRouter><SideMenu onClose={vi.fn()} /></MemoryRouter>)

    expect(screen.getByRole('link', { name: 'Carrinho' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Favoritos' })).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Compras recebidas' })).not.toBeInTheDocument()
  })

  it('mostra vendas e produtos para conta SEBO e esconde carrinho', () => {
    authenticate(account(AccountType.SEBO))
    render(<MemoryRouter><SideMenu onClose={vi.fn()} /></MemoryRouter>)

    expect(screen.getByRole('link', { name: 'Meus Produtos' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Compras recebidas' })).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Carrinho' })).not.toBeInTheDocument()
  })
})

function renderRoute(currentAccount: Account | null, initialPath: string, role?: AccountType) {
  if (currentAccount) authenticate(currentAccount)
  return render(
    <MemoryRouter initialEntries={[initialPath]}>
      <Routes>
        <Route path="/" element={<div>Inicio</div>} />
        <Route path="/auth" element={<div>Autenticacao</div>} />
        <Route
          path={initialPath}
          element={<ProtectedRoute requireRole={role}><div>Conteudo protegido</div></ProtectedRoute>}
        />
      </Routes>
    </MemoryRouter>,
  )
}

function authenticate(currentAccount: Account) {
  useAuthStore.setState({ token: 'token', account: currentAccount, seboId: null })
}

function account(type: AccountType): Account {
  return { id: 1, name: 'Conta Teste', username: 'conta.teste', email: 'conta@teste.com', type }
}
