import { Link } from 'react-router-dom'
import './TopBar.css'

type Props = {
  message?: string
}

export function TopBar({
  message = 'Livros, discos, cds, eletrônicos, colecionáveis e muito mais no Vitral, seu sebo virtual.',
}: Props) {
  return (
    <div className="topbar">
      <div className="topbar-inner container">
        <span className="topbar-message">{message}</span>
        <Link to="/ajuda" className="topbar-help">Ajuda</Link>
      </div>
    </div>
  )
}
