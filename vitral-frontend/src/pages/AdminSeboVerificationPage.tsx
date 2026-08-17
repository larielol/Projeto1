import { useCallback, useEffect, useState } from 'react'
import { StorePage, StoreStatus } from '../components/Layout/StorePage'
import { Button } from '../components/ui/Button'
import { Pagination } from '../components/ui/Pagination'
import { extractErrorMessage } from '../services/api'
import { seboService } from '../services/seboService'
import { StatusVerificacao, type AuditoriaSebo, type DocumentoSebo, type Sebo } from '../types/sebo'
import { documentLabels, formatBytes } from '../utils/documentSebo'
import { abrirDocumentoAutenticado } from '../utils/documentoAutenticado'
import './StockMovementsPage.css'

const cnpj = (value?: string) => value ? value.replace(/^(\d{2})(\d{3})(\d{3})(\d{4})(\d{2})$/, '$1.$2.$3/$4-$5') : 'CNPJ não informado'
const date = new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeStyle: 'short' })

export function AdminSeboVerificationPage() {
  const [items, setItems] = useState<Sebo[]>([])
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [loading, setLoading] = useState(true)
  const [docs, setDocs] = useState<Record<number, DocumentoSebo[]>>({})
  const [audits, setAudits] = useState<Record<number, AuditoriaSebo[]>>({})
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)

  const load = useCallback(() => {
    setLoading(true)
    seboService.listarPendentes(page, 20)
      .then((result) => {
        setItems(result.content)
        setTotalPages(result.totalPages)
        setError(null)
      })
      .catch((e) => setError(extractErrorMessage(e, 'Não foi possível carregar os sebos pendentes.')))
      .finally(() => setLoading(false))
  }, [page])

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    load()
  }, [load])

  async function details(id: number) {
    try {
      const [documents, audit] = await Promise.all([
        seboService.listarDocumentos(id),
        seboService.listarAuditoria(id),
      ])
      setDocs((current) => ({ ...current, [id]: documents }))
      setAudits((current) => ({ ...current, [id]: audit }))
    } catch (e) {
      setError(extractErrorMessage(e, 'Não foi possível carregar os detalhes.'))
    }
  }

  async function review(item: Sebo, status: StatusVerificacao) {
    if (!confirm(`${status === StatusVerificacao.VERIFICADO ? 'Aprovar' : 'Rejeitar'} o sebo ${item.nome}?`)) return
    let motivo: string | undefined
    if (status === StatusVerificacao.REJEITADO) {
      motivo = prompt('Informe o motivo obrigatório da rejeição:')?.trim()
      if (!motivo) {
        setError('O motivo é obrigatório para rejeitar.')
        return
      }
    }
    try {
      await seboService.atualizarVerificacao(item.id, status, motivo)
      setSuccess(`Sebo ${status === StatusVerificacao.VERIFICADO ? 'aprovado' : 'rejeitado'} com sucesso.`)
      load()
    } catch (e) {
      setError(extractErrorMessage(e, 'Não foi possível revisar o sebo.'))
    }
  }

  async function handleAbrir(url: string) {
    try {
      await abrirDocumentoAutenticado(url)
    } catch (e) {
      setError(extractErrorMessage(e, 'Não foi possível abrir o documento.'))
    }
  }

  return (
    <StorePage title="Verificação de sebos" subtitle="Analise documentos e aprove ou rejeite cadastros pendentes.">
      {error ? <div className="flash error" role="alert">{error}</div> : null}
      {success ? <div className="flash success" role="status">{success}</div> : null}
      {loading ? (
        <StoreStatus title="Carregando pendentes" description="Consultando sebos em análise." busy />
      ) : items.length === 0 ? (
        <StoreStatus title="Nenhum sebo pendente" description="Não há cadastros aguardando análise." />
      ) : (
        <div className="admin-sebo-list">
          {items.map((item) => (
            <article className="auth-card" key={item.id}>
              <h2>{item.nome}</h2>
              <p>{item.email} · {cnpj(item.cnpj)}</p>
              <p>{item.descricao || 'Sem descrição'} · {item.telefone || 'Sem telefone'}</p>
              <div className="form-actions">
                <Button type="button" variant="ghost" onClick={() => details(item.id)}>Ver documentos e auditoria</Button>
                <Button type="button" onClick={() => review(item, StatusVerificacao.VERIFICADO)}>Aprovar</Button>
                <Button type="button" variant="danger" onClick={() => review(item, StatusVerificacao.REJEITADO)}>Rejeitar</Button>
              </div>
              {docs[item.id] ? (
                <>
                  <h3>Documentos</h3>
                  {docs[item.id].length === 0 ? <p>Nenhum documento enviado.</p> : (
                    <div className="stock-table-scroll">
                      <table className="stock-table">
                        <thead>
                          <tr>
                            <th>Nome</th>
                            <th>Tipo</th>
                            <th>Tamanho</th>
                            <th>Content type</th>
                            <th>Data de envio</th>
                            <th>Status</th>
                            <th>Arquivo</th>
                          </tr>
                        </thead>
                        <tbody>
                          {docs[item.id].map((document) => (
                            <tr key={document.id}>
                              <td>{document.nomeArquivo || 'Documento'}</td>
                              <td>{documentLabels[document.tipo] ?? document.tipo}</td>
                              <td>{formatBytes(document.tamanhoBytes)}</td>
                              <td>{document.contentType || '—'}</td>
                              <td>{date.format(new Date(document.enviadoEm))}</td>
                              <td>{document.status}</td>
                              <td><button type="button" className="link-button" onClick={() => handleAbrir(document.arquivoUrl)}>Abrir</button></td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  )}
                  <h3>Auditoria</h3>
                  {(audits[item.id] || []).map((audit) => (
                    <p key={audit.id}>
                      Admin #{audit.analisadoPorId}: {audit.statusAnterior} → {audit.novoStatus} · {audit.motivo || 'Sem motivo'} · {date.format(new Date(audit.criadoEm))}
                    </p>
                  ))}
                </>
              ) : null}
            </article>
          ))}
        </div>
      )}
      <Pagination page={page} totalPages={totalPages} onPageChange={setPage} label="Páginas de sebos pendentes" />
    </StorePage>
  )
}
