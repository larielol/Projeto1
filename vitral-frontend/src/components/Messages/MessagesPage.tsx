import { useEffect, useMemo, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'
import { StorePage, StoreStatus } from '../Layout/StorePage'
import { Pagination } from '../ui/Pagination'
import { useAuth } from '../../hooks/useAuth'
import { extractErrorMessage } from '../../services/api'
import { mensagemService } from '../../services/mensagemService'
import { fetchAllPages } from '../../services/pagination'
import type { Mensagem } from '../../types/mensagem'

type ConversaResumo = {
  accountId: number
  nome: string
  ultimaMensagem: string
  data: string
}

export function MessagesPage() {
  const { account } = useAuth()
  const [searchParams] = useSearchParams()
  const initialAccountIdParam = searchParams.get('destinatarioId')
  const initialAccountId = initialAccountIdParam ? Number(initialAccountIdParam) : null
  const initialName = searchParams.get('nome') ?? ''
  const hasInitialConversation = Boolean(initialAccountId && Number.isFinite(initialAccountId))

  const [messages, setMessages] = useState<Mensagem[]>([])
  const [conversation, setConversation] = useState<Mensagem[]>([])
  const [selectedAccountId, setSelectedAccountId] = useState<number | null>(
    hasInitialConversation ? initialAccountId : null,
  )
  const [selectedName, setSelectedName] = useState(initialName)
  const [content, setContent] = useState('')
  const [loading, setLoading] = useState(true)
  const [conversationLoading, setConversationLoading] = useState(hasInitialConversation)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [sendError, setSendError] = useState<string | null>(null)
  const [conversationPage, setConversationPage] = useState(0)
  const conversationsPerPage = 10

  const conversations = useMemo<ConversaResumo[]>(() => {
    const map = new Map<number, ConversaResumo>()
    for (const message of messages) {
      const otherId = message.remetenteId === account?.id ? message.destinatarioId : message.remetenteId
      const otherName = message.remetenteId === account?.id ? message.destinatarioNome : message.remetenteNome
      if (!map.has(otherId)) {
        map.set(otherId, {
          accountId: otherId,
          nome: otherName,
          ultimaMensagem: message.conteudo,
          data: message.createdAt,
        })
      }
    }
    return Array.from(map.values())
  }, [account?.id, messages])
  const visibleConversations = conversations.slice(
    conversationPage * conversationsPerPage,
    (conversationPage + 1) * conversationsPerPage,
  )
  const totalConversationPages = Math.ceil(conversations.length / conversationsPerPage)

  useEffect(() => {
    let active = true
    fetchAllPages((page, size) => page === 0 ? mensagemService.listar() : mensagemService.listar(page, size))
      .then((items) => {
        if (!active) return
        setMessages(items)
        setError(null)
      })
      .catch((err) => {
        if (active) setError(extractErrorMessage(err, 'Nao foi possivel carregar suas mensagens.'))
      })
      .finally(() => {
        if (active) setLoading(false)
      })

    return () => {
      active = false
    }
  }, [])

  useEffect(() => {
    let active = true
    if (!selectedAccountId) {
      return
    }

    fetchAllPages((page, size) => page === 0
      ? mensagemService.listarConversa(selectedAccountId)
      : mensagemService.listarConversa(selectedAccountId, page, size))
      .then((items) => {
        if (!active) return
        setConversation(items)
        setSendError(null)
      })
      .catch((err) => {
        if (active) setSendError(extractErrorMessage(err, 'Nao foi possivel carregar a conversa.'))
      })
      .finally(() => {
        if (active) setConversationLoading(false)
      })

    return () => {
      active = false
    }
  }, [selectedAccountId])

  function selectConversation(conversationSummary: ConversaResumo) {
    setConversationLoading(true)
    setSelectedAccountId(conversationSummary.accountId)
    setSelectedName(conversationSummary.nome)
  }

  async function sendMessage(event: React.FormEvent) {
    event.preventDefault()
    if (!selectedAccountId || !content.trim()) return

    setBusy(true)
    setSendError(null)
    try {
      const sent = await mensagemService.enviar({
        destinatarioId: selectedAccountId,
        conteudo: content.trim(),
      })
      setConversation((current) => [...current, sent])
      setMessages((current) => [sent, ...current])
      setContent('')
      if (!selectedName) setSelectedName(sent.destinatarioNome)
    } catch (err) {
      setSendError(extractErrorMessage(err, 'Nao foi possivel enviar a mensagem.'))
    } finally {
      setBusy(false)
    }
  }

  return (
    <StorePage title="Mensagens" subtitle="Converse diretamente com vendedores e compradores.">
      {loading ? (
        <StoreStatus title="Carregando mensagens" description="Buscando suas conversas." busy />
      ) : error ? (
        <StoreStatus title="Erro ao carregar mensagens" description={error} tone="error" />
      ) : conversations.length === 0 && !selectedAccountId ? (
        <StoreStatus
          title="Nenhuma conversa ainda"
          description="Abra o perfil de um sebo para iniciar uma conversa."
          action={<Link to="/busca?tab=sebos">Encontrar sebos</Link>}
        />
      ) : (
        <section className="store-messages" aria-label="Mensagens">
          <aside className="store-message-list">
            {visibleConversations.map((item) => (
              <button
                className={item.accountId === selectedAccountId ? 'is-selected' : ''}
                key={item.accountId}
                type="button"
                onClick={() => selectConversation(item)}
              >
                <strong>{item.nome}</strong>
                <span>{item.ultimaMensagem}</span>
              </button>
            ))}
            <Pagination
              page={conversationPage}
              totalPages={totalConversationPages}
              onPageChange={setConversationPage}
              label="Páginas de conversas"
            />
          </aside>

          <article className="store-message-panel">
            <header>
              <h2>{selectedName || 'Nova conversa'}</h2>
              {selectedAccountId ? <p>Conversa particular</p> : null}
            </header>

            {conversationLoading ? (
              <p className="store-message-note">Carregando conversa...</p>
            ) : conversation.length === 0 ? (
              <p className="store-message-note">Envie a primeira mensagem para iniciar a conversa.</p>
            ) : (
              <div className="store-message-thread">
                {conversation.map((message) => (
                  <p
                    className={message.remetenteId === account?.id ? 'sent' : 'received'}
                    key={message.id}
                  >
                    {message.conteudo}
                  </p>
                ))}
              </div>
            )}

            {sendError ? <div className="flash error" role="alert">{sendError}</div> : null}
            <form className="store-message-compose" onSubmit={sendMessage}>
              <textarea
                aria-label="Mensagem"
                placeholder={selectedAccountId ? 'Escreva uma mensagem...' : 'Selecione uma conversa'}
                value={content}
                onChange={(event) => setContent(event.target.value)}
                disabled={!selectedAccountId || busy}
                rows={4}
              />
              <button type="submit" disabled={!selectedAccountId || !content.trim() || busy}>
                {busy ? 'Enviando...' : 'Enviar'}
              </button>
            </form>
          </article>
        </section>
      )}
    </StorePage>
  )
}
