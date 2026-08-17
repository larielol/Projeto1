import { Link } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth'
import './SideMenu.css'

type Props = {
  onClose: () => void
}

const publicItems = [
  { label: 'Categorias', to: '/categorias' },
  { label: 'Ofertas', to: '/ofertas' },
  { label: 'Lançamentos', to: '/lancamentos' },
  { label: 'Suporte', to: '/suporte' },
]

const userItems = [
  { label: 'Recomendados para você', to: '/recomendacoes' },
  { label: 'Carrinho', to: '/carrinho' },
  { label: 'Favoritos', to: '/favoritos' },
  { label: 'Reservas', to: '/reservas' },
  { label: 'Histórico', to: '/historico' },
  { label: 'Mensagens', to: '/mensagens' },
]

const seboItems = [
  { label: 'Compras recebidas', to: '/vendas' },
  { label: 'Histórico de vendas', to: '/vendas/historico' },
  { label: 'Mensagens', to: '/mensagens' },
]

export function SideMenu({ onClose }: Props) {
  const { isAuthenticated, isSebo, isAdmin } = useAuth()

  return (
    <>
      <div className="sidemenu-backdrop" onClick={onClose} />
      <aside className="sidemenu" id="main-side-menu" aria-label="Menu principal">
        <div className="sidemenu-header">
          <strong>Menu</strong>
          <button type="button" className="sidemenu-close" onClick={onClose} aria-label="Fechar menu">
            ×
          </button>
        </div>
        <nav aria-label="Navegação principal">
          <ul>
            {!isAuthenticated ? (
              <li>
                <Link to="/auth" onClick={onClose}>Entrar</Link>
              </li>
            ) : isAdmin ? (
              <li><Link to="/admin/sebos" onClick={onClose}>Verificar Sebos</Link></li>
            ) : isSebo ? (
              <>
                <li>
                  <Link to="/painel/sebo" onClick={onClose}>Meu Sebo</Link>
                </li>
                <li>
                  <Link to="/painel/produtos" onClick={onClose}>Meus Produtos</Link>
                </li>
                <li>
                  <Link to="/painel/ofertas" onClick={onClose}>Minhas Ofertas</Link>
                </li>
                <li><Link to="/painel/sebo/documentos" onClick={onClose}>Documentos do sebo</Link></li>
              </>
            ) : (
              <li>
                <Link to="/painel/perfil" onClick={onClose}>Meu Perfil</Link>
              </li>
            )}

            {isAuthenticated && !isSebo
              ? userItems.map((item) => (
                <li key={item.label}>
                  <Link to={item.to} onClick={onClose}>
                    {item.label}
                  </Link>
                </li>
              ))
              : null}

            {isAuthenticated && isSebo
              ? seboItems.map((item) => (
                <li key={item.label}>
                  <Link to={item.to} onClick={onClose}>
                    {item.label}
                  </Link>
                </li>
              ))
              : null}

            {publicItems.map((item) => (
              <li key={item.label}>
                <Link to={item.to} onClick={onClose}>
                  {item.label}
                </Link>
              </li>
            ))}
          </ul>
        </nav>
      </aside>
    </>
  )
}
