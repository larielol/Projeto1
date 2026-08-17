import {
  CardIcon,
  HeartIcon,
  InfoIcon,
  MinusCircleIcon,
} from '../Layout/Icons'
import './HelpPage.css'

const helpItems = [
  {
    icon: <InfoIcon size={33} />,
    content: (
      <>
        O Vitral é um sebo virtual, ideal para compras no conforto de sua casa e com a agilidade que
        precisamos.
        <br />É simples, rápido e fácil: basta se cadastrar, escolher o que quiser e solicitar reserva ao
        vendedor.
      </>
    ),
  },
  {
    icon: <HeartIcon size={35} />,
    content: 'Adicione o item que quiser na sua lista de favoritos.',
  },
  {
    icon: <CardIcon size={34} />,
    content: 'Verifique a possibilidade de pagamentos, entregas e reservas diretamente com o vendedor.',
  },
  {
    icon: <MinusCircleIcon size={34} />,
    content: 'Denuncia para a análise interna do caso.',
  },
]

export function HelpPage() {
  return (
    <main className="help-page">
      <section className="help-content" aria-label="Como usar o Vitral">
        {helpItems.map((item, index) => (
          <article className="help-item" key={index}>
            <span className="help-icon">{item.icon}</span>
            <p>{item.content}</p>
          </article>
        ))}
      </section>
    </main>
  )
}
