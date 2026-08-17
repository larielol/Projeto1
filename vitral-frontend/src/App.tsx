import { BrowserRouter, Route, Routes } from 'react-router-dom'
import { CartPage } from './components/Cart/CartPage'
import { CategoriesPage } from './components/Categories/CategoriesPage'
import { CheckoutPage } from './components/Checkout/CheckoutPage'
import { DeleteAccountPage } from './components/DeleteAccount/DeleteAccountPage'
import { FavoritesPage } from './components/Favorites/FavoritesPage'
import { HelpPage } from './components/Help/HelpPage'
import { HistoryPage } from './components/History/HistoryPage'
import { MessagesPage } from './components/Messages/MessagesPage'
import { OffersPage } from './components/Offers/OffersPage'
import { ProductPage } from './components/Product/ProductPage'
import { ReleasesPage } from './components/Releases/ReleasesPage'
import { ReservationsPage } from './components/Reservations/ReservationsPage'
import { SaleConfirmationPage } from './components/Sales/SaleConfirmationPage'
import { SalesHistoryPage } from './components/Sales/SalesHistoryPage'
import { SalesOrdersPage } from './components/Sales/SalesOrdersPage'
import { SellerPage } from './components/Seller/SellerPage'
import { SupportPage } from './components/Support/SupportPage'
import { AuthPage } from './pages/AuthPage'
import { BuscaPage } from './pages/BuscaPage'
import { ConfirmarEmailPage } from './pages/ConfirmarEmailPage'
import { HomePage } from './pages/HomePage'
import { ForgotPasswordPage } from './pages/ForgotPasswordPage'
import { ResetPasswordPage } from './pages/ResetPasswordPage'
import { ResendConfirmationPage } from './pages/ResendConfirmationPage'
import { MeuSeboPage } from './pages/MeuSeboPage'
import { MeusProdutosPage } from './pages/MeusProdutosPage'
import { MinhasOfertasPage } from './pages/MinhasOfertasPage'
import { NotFoundPage } from './pages/NotFoundPage'
import { PerfilPage } from './pages/PerfilPage'
import { RecomendacoesPage } from './pages/RecomendacoesPage'
import { ProtectedRoute } from './routes/ProtectedRoute'
import { AccountType } from './types/account'
import { GlobalNavigation } from './components/GlobalNavigation'
import { SeboDocumentsPage } from './pages/SeboDocumentsPage'
import { AdminSeboVerificationPage } from './pages/AdminSeboVerificationPage'

function App() {
  return (
    <BrowserRouter>
      <GlobalNavigation />
      <Routes>
        <Route path="/" element={<HomePage />} />
        <Route path="/auth" element={<AuthPage />} />
        <Route path="/entrar" element={<AuthPage />} />
        <Route path="/auth/confirmar" element={<ConfirmarEmailPage />} />
        <Route path="/auth/recuperar-senha" element={<ForgotPasswordPage />} />
        <Route path="/auth/redefinir-senha" element={<ResetPasswordPage />} />
        <Route path="/auth/reenviar-confirmacao" element={<ResendConfirmationPage />} />
        <Route path="/busca" element={<BuscaPage />} />
        <Route path="/buscar" element={<BuscaPage />} />

        <Route
          path="/painel/perfil"
          element={
            <ProtectedRoute>
              <PerfilPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/painel/sebo"
          element={
            <ProtectedRoute requireRole={AccountType.SEBO}>
              <MeuSeboPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/painel/produtos"
          element={
            <ProtectedRoute requireRole={AccountType.SEBO}>
              <MeusProdutosPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/painel/ofertas"
          element={
            <ProtectedRoute requireRole={AccountType.SEBO}>
              <MinhasOfertasPage />
            </ProtectedRoute>
          }
        />
        <Route path="/painel/sebo/documentos" element={<ProtectedRoute requireRole={AccountType.SEBO}><SeboDocumentsPage /></ProtectedRoute>} />
        <Route path="/admin/sebos" element={<ProtectedRoute requireRole={AccountType.ADMIN}><AdminSeboVerificationPage /></ProtectedRoute>} />

        <Route
          path="/conta"
          element={
            <ProtectedRoute>
              <PerfilPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/conta/excluir"
          element={
            <ProtectedRoute>
              <DeleteAccountPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/carrinho"
          element={
            <ProtectedRoute>
              <CartPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/confirmar-pedido"
          element={
            <ProtectedRoute>
              <CheckoutPage />
            </ProtectedRoute>
          }
        />
        <Route path="/categorias" element={<CategoriesPage />} />
        <Route
          path="/favoritos"
          element={
            <ProtectedRoute>
              <FavoritesPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/recomendacoes"
          element={
            <ProtectedRoute requireRole={AccountType.USUARIO}>
              <RecomendacoesPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/historico"
          element={
            <ProtectedRoute>
              <HistoryPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/anuncios"
          element={
            <ProtectedRoute requireRole={AccountType.SEBO}>
              <MeusProdutosPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/mensagens"
          element={
            <ProtectedRoute>
              <MessagesPage />
            </ProtectedRoute>
          }
        />
        <Route path="/ofertas" element={<OffersPage />} />
        <Route path="/produto/:productId" element={<ProductPage />} />
        <Route
          path="/reservas"
          element={
            <ProtectedRoute>
              <ReservationsPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/sebo/perfil"
          element={
            <ProtectedRoute requireRole={AccountType.SEBO}>
              <MeuSeboPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/vendas"
          element={
            <ProtectedRoute requireRole={AccountType.SEBO}>
              <SalesOrdersPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/vendas/historico"
          element={
            <ProtectedRoute requireRole={AccountType.SEBO}>
              <SalesHistoryPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/vendas/:orderId/confirmar"
          element={
            <ProtectedRoute requireRole={AccountType.SEBO}>
              <SaleConfirmationPage />
            </ProtectedRoute>
          }
        />
        <Route path="/vendedor/:sellerId" element={<SellerPage />} />
        <Route path="/lancamentos" element={<ReleasesPage />} />
        <Route
          path="/suporte"
          element={
            <ProtectedRoute>
              <SupportPage />
            </ProtectedRoute>
          }
        />
        <Route path="/ajuda" element={<HelpPage />} />
        <Route path="*" element={<NotFoundPage />} />
      </Routes>
    </BrowserRouter>
  )
}

export default App
