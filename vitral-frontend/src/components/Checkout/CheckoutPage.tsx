import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import { StorePage, StoreStatus } from '../Layout/StorePage'
import { extractErrorMessage, extractProfileIncompleteError, type ProfileIncompleteError } from '../../services/api'
import { cestaService } from '../../services/cestaService'
import { pedidoService } from '../../services/pedidoService'
import type { CestaItem } from '../../types/cesta'
import { FormaPagamento, type Pedido } from '../../types/pedido'

export function CheckoutPage() {
  const [items, setItems] = useState<CestaItem[]>([])
  const [pedido, setPedido] = useState<Pedido | null>(null)
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [profileIssue, setProfileIssue] = useState<ProfileIncompleteError | null>(null)
  const [formaPagamento, setFormaPagamento] = useState<FormaPagamento>(FormaPagamento.PIX)
  const [numeroCartao, setNumeroCartao] = useState('')

  const total = useMemo(
    () => items.reduce((sum, item) => sum + Number(item.subtotal), 0),
    [items],
  )
  const totalItems = useMemo(
    () => items.reduce((sum, item) => sum + Number(item.quantidade), 0),
    [items],
  )

  useEffect(() => {
    let active = true
    cestaService
      .listar()
      .then((data) => {
        if (!active) return
        setItems(data)
        setError(null)
      })
      .catch((err) => {
        if (active) setError(extractErrorMessage(err, 'Nao foi possivel carregar sua cesta.'))
      })
      .finally(() => {
        if (active) setLoading(false)
      })

    return () => {
      active = false
    }
  }, [])

  async function confirmarPedido() {
    setBusy(true)
    setError(null)
    setProfileIssue(null)
    try {
      const criado = await pedidoService.confirmarPedido({
        formaPagamento,
        numeroCartao: formaPagamento === FormaPagamento.CARTAO ? numeroCartao : undefined,
      })
      setPedido(criado)
      setItems([])
    } catch (err) {
      const incompleteProfile = extractProfileIncompleteError(err)
      if (incompleteProfile) setProfileIssue(incompleteProfile)
      else setError(extractErrorMessage(err, 'Nao foi possivel confirmar o pedido.'))
    } finally {
      setBusy(false)
    }
  }

  return (
    <StorePage title="Confirmar pedido" subtitle="Envie os itens da cesta para confirmação do sebo.">
      {loading ? (
        <StoreStatus title="Carregando pedido" description="Buscando os itens da sua cesta." busy />
      ) : pedido ? (
        <StoreStatus
          title={`Pedido #${pedido.id} enviado`}
          description="Pagamento aprovado. Seu pedido foi enviado ao sebo e está aguardando confirmação."
          tone="success"
          action={<Link to="/reservas">Acompanhar pedido</Link>}
        />
      ) : error && items.length === 0 ? (
        <StoreStatus
          title="Erro ao carregar carrinho"
          description={error}
          tone="error"
          action={<Link to="/carrinho">Voltar para o carrinho</Link>}
        />
      ) : items.length === 0 ? (
        <StoreStatus
          title="Sua cesta está vazia"
          description="Adicione produtos ao carrinho antes de confirmar um pedido."
          action={<Link to="/busca">Buscar produtos</Link>}
        />
      ) : (
        <>
          {profileIssue ? (
            <div className="flash error checkout-profile-error" role="alert">
              <strong>Complete seu perfil antes de finalizar a compra.</strong>
              <p>{profileIssue.message}</p>
              {profileIssue.fields.length > 0 ? (
                <ul>
                  {profileIssue.fields.map((item) => <li key={item.field}>{item.field}: {item.message}</li>)}
                </ul>
              ) : null}
              <Link
                to="/painel/perfil?from=checkout"
                state={{ fromCheckout: true, incompleteFields: profileIssue.fields.map((item) => item.field) }}
              >
                Completar perfil
              </Link>
            </div>
          ) : null}
          {error ? <div className="flash error" role="alert">{error}</div> : null}
          <section className="store-summary" aria-label="Resumo e pagamento">
            <div>
              <p>{totalItems} {totalItems === 1 ? 'item' : 'itens'} para pagar</p>
              <strong>R$ {total.toFixed(2)}</strong>
            </div>
            <div className="store-payment">
              <label className="store-payment-field">
                <span>Forma de pagamento</span>
                <select
                  value={formaPagamento}
                  onChange={(e) => setFormaPagamento(e.target.value as FormaPagamento)}
                >
                  <option value={FormaPagamento.PIX}>PIX</option>
                  <option value={FormaPagamento.CARTAO}>Cartão de crédito</option>
                  <option value={FormaPagamento.BOLETO}>Boleto</option>
                </select>
              </label>
              {formaPagamento === FormaPagamento.CARTAO ? (
                <label className="store-payment-field">
                  <span>Número do cartão</span>
                  <input
                    type="text"
                    inputMode="numeric"
                    autoComplete="off"
                    placeholder="0000 0000 0000 0000"
                    value={numeroCartao}
                    onChange={(e) => setNumeroCartao(e.target.value)}
                  />
                </label>
              ) : null}
              <p className="store-payment-note">Pagamento simulado para fins de demonstração (MVP).</p>
            </div>
            <button className="primary" type="button" disabled={busy} onClick={confirmarPedido}>
              {busy ? 'Processando pagamento...' : 'Pagar e confirmar'}
            </button>
          </section>

          <section className="store-list" aria-label="Itens do pedido">
            {items.map((item) => (
              <article className="store-list-card" key={item.id}>
                <Link className="store-list-image" to={`/produto/${item.produtoId}`}>
                  {item.fotoUrl ? <img src={item.fotoUrl} alt={item.titulo} /> : 'Sem imagem'}
                </Link>
                <div className="store-list-copy">
                  <h2>{item.titulo}</h2>
                  <p>{item.autor ?? 'Autor nao informado'} · {item.condicao} · Qtd. {item.quantidade}</p>
                  <strong className="store-list-price">R$ {Number(item.subtotal).toFixed(2)}</strong>
                </div>
              </article>
            ))}
          </section>
        </>
      )}
    </StorePage>
  )
}
