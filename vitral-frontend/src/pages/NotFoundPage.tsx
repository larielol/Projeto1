import { Link } from 'react-router-dom'

export function NotFoundPage() {
  return (
    <div className="page">
      <main className="container" style={{ padding: '60px 0', textAlign: 'center' }}>
        <h1>Página não encontrada</h1>
        <p style={{ margin: '16px 0' }}>O endereço acessado não existe.</p>
        <Link to="/">Voltar para a home</Link>
      </main>
    </div>
  )
}
