import { Component, type ErrorInfo, type ReactNode } from 'react'
import logo from '../assets/vitral-sebo-icon.svg'
import './ErrorBoundary.css'

type Props = { children: ReactNode }
type State = { hasError: boolean }

export class ErrorBoundary extends Component<Props, State> {
  state: State = { hasError: false }

  static getDerivedStateFromError(): State {
    return { hasError: true }
  }

  componentDidCatch(error: Error, info: ErrorInfo) {
    console.error('Erro inesperado na interface', error, info)
  }

  render() {
    if (!this.state.hasError) return this.props.children

    return (
      <main className="error-boundary" role="alert">
        <img src={logo} alt="Vitral" />
        <h1>Algo não saiu como esperado</h1>
        <p>Você pode tentar novamente ou voltar para a página inicial.</p>
        <div>
          <button type="button" onClick={() => window.location.reload()}>Tentar novamente</button>
          <a href="/">Voltar ao início</a>
        </div>
      </main>
    )
  }
}
