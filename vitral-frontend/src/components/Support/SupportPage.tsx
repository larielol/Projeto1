import { useState, type FormEvent } from 'react'
import { StorePage } from '../Layout/StorePage'
import { enviarSuporte } from '../../services/suporteService'
import { extractErrorMessage } from '../../services/api'
import './SupportPage.css'

const questions = [
  'Como faço uma reserva?',
  'Como conversar com um vendedor?',
  'Posso denunciar um anúncio?',
  'Como altero os dados da minha conta?',
]

export function SupportPage() {
  const [assunto, setAssunto] = useState('')
  const [mensagem, setMensagem] = useState('')
  const [loading, setLoading] = useState(false)
  const [sucesso, setSucesso] = useState(false)
  const [erro, setErro] = useState<string | null>(null)

  async function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setErro(null)
    setSucesso(false)

    if (!assunto.trim() || !mensagem.trim()) {
      setErro('Preencha o assunto e a mensagem antes de enviar.')
      return
    }

    setLoading(true)
    try {
      await enviarSuporte({ assunto: assunto.trim(), mensagem: mensagem.trim() })
      setSucesso(true)
      setAssunto('')
      setMensagem('')
    } catch (err) {
      setErro(extractErrorMessage(err, 'Não foi possível enviar sua mensagem. Tente novamente.'))
    } finally {
      setLoading(false)
    }
  }

  return (
    <StorePage
      title="Suporte"
      subtitle="Encontre respostas ou fale com a equipe do Vitral."
      searchPlaceholder="Como podemos ajudar?"
    >
      <section className="support-shortcuts" aria-label="Canais de suporte">
        <article>
          <span>01</span>
          <h2>Central de ajuda</h2>
          <p>Consulte orientações para comprar, reservar e usar sua conta.</p>
        </article>
        <article>
          <span>02</span>
          <h2>Fale conosco</h2>
          <p>Envie uma mensagem para nossa equipe analisar sua solicitação.</p>
        </article>
        <article>
          <span>03</span>
          <h2>Segurança</h2>
          <p>Informe anúncios ou comportamentos que precisam de atenção.</p>
        </article>
      </section>

      <section className="support-main">
        <div className="support-faq">
          <h2>Perguntas frequentes</h2>
          {questions.map((question) => (
            <details key={question}>
              <summary>{question}</summary>
              <p>
                Acesse a área correspondente pelo menu principal. Caso ainda precise de ajuda, envie sua
                dúvida pelo formulário.
              </p>
            </details>
          ))}
        </div>

        <form className="support-form" onSubmit={handleSubmit} noValidate>
          <h2>Envie sua dúvida</h2>

          <label htmlFor="support-subject">Assunto</label>
          <input
            id="support-subject"
            placeholder="Informe o assunto"
            value={assunto}
            onChange={(e) => setAssunto(e.target.value)}
            disabled={loading}
          />

          <label htmlFor="support-message">Mensagem</label>
          <textarea
            id="support-message"
            rows={5}
            placeholder="Conte o que aconteceu"
            value={mensagem}
            onChange={(e) => setMensagem(e.target.value)}
            disabled={loading}
          />

          {erro && <p className="support-feedback support-feedback--erro" role="alert">{erro}</p>}
          {sucesso && (
            <p className="support-feedback support-feedback--sucesso" role="status">
              Mensagem enviada com sucesso! Nossa equipe entrará em contato em breve.
            </p>
          )}

          <button type="submit" disabled={loading}>
            {loading ? 'Enviando...' : 'Enviar mensagem'}
          </button>
        </form>
      </section>
    </StorePage>
  )
}
