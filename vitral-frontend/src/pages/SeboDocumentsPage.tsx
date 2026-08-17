import { useCallback, useEffect, useState } from 'react'
import { StorePage, StoreStatus } from '../components/Layout/StorePage'
import { Button } from '../components/ui/Button'
import { extractErrorMessage } from '../services/api'
import { seboService } from '../services/seboService'
import { TipoDocumentoSebo, type DocumentoSebo } from '../types/sebo'
import { documentLabels, formatBytes } from '../utils/documentSebo'
import { abrirDocumentoAutenticado } from '../utils/documentoAutenticado'
import './StockMovementsPage.css'

const date = new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeStyle: 'short' })

export function SeboDocumentsPage() {
  const [items, setItems] = useState<DocumentoSebo[]>([])
  const [loading, setLoading] = useState(true)
  const [tipo, setTipo] = useState<TipoDocumentoSebo>(TipoDocumentoSebo.CARTAO_CNPJ)
  const [arquivo, setArquivo] = useState<File | null>(null)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)

  const load = useCallback(() => {
    setLoading(true)
    seboService.listarMeusDocumentos()
      .then((data) => {
        setItems(data)
        setError(null)
      })
      .catch((e) => setError(extractErrorMessage(e, 'Não foi possível carregar os documentos.')))
      .finally(() => setLoading(false))
  }, [])

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    load()
  }, [load])

  async function submit(e: React.FormEvent) {
    e.preventDefault()
    if (!arquivo) {
      setError('Selecione um arquivo para enviar.')
      return
    }
    setBusy(true)
    setError(null)
    setSuccess(null)
    try {
      await seboService.enviarDocumento(tipo, arquivo)
      setArquivo(null)
      setSuccess('Documento enviado. A verificação retornou para PENDENTE.')
      load()
    } catch (err) {
      setError(extractErrorMessage(err, 'Não foi possível enviar o documento.'))
    } finally {
      setBusy(false)
    }
  }

  async function handleAbrir(url: string) {
    try {
      await abrirDocumentoAutenticado(url)
    } catch (err) {
      setError(extractErrorMessage(err, 'Não foi possível abrir o documento.'))
    }
  }

  return (
    <StorePage title="Documentos do sebo" subtitle="Envie os documentos necessários para a verificação.">
      <p className="flash">O envio de um novo documento retorna a verificação do sebo para PENDENTE.</p>
      {error ? <div className="flash error" role="alert">{error}</div> : null}
      {success ? <div className="flash success" role="status">{success}</div> : null}

      <form className="form-grid" onSubmit={submit}>
        <label className="vit-input-wrapper">
          <span className="vit-input-label">Tipo do documento</span>
          <select className="vit-input" value={tipo} onChange={(e) => setTipo(e.target.value as TipoDocumentoSebo)}>
            {Object.entries(documentLabels).map(([value, label]) => (
              <option key={value} value={value}>{label}</option>
            ))}
          </select>
        </label>
        <label className="vit-input-wrapper">
          <span className="vit-input-label">Arquivo</span>
          <input
            className="vit-input"
            type="file"
            accept="application/pdf,image/png,image/jpeg,image/jpg"
            onChange={(event) => setArquivo(event.target.files?.[0] ?? null)}
            required
          />
        </label>
        <p className="image-upload-hint">PDF, PNG ou JPG conforme limite aceito pelo backend.</p>
        <Button type="submit" disabled={busy || !arquivo}>{busy ? 'Enviando...' : 'Enviar documento'}</Button>
      </form>

      <h2>Documentos enviados</h2>
      {loading ? (
        <StoreStatus title="Carregando documentos" description="Consultando os documentos enviados." busy />
      ) : items.length === 0 ? (
        <StoreStatus title="Nenhum documento enviado" description="Envie o primeiro documento usando o formulário acima." />
      ) : (
        <div className="stock-table-scroll">
          <table className="stock-table">
            <thead>
              <tr>
                <th>Tipo</th>
                <th>Arquivo</th>
                <th>Content type</th>
                <th>Tamanho</th>
                <th>Enviado em</th>
                <th>Status</th>
                <th>Motivo</th>
                <th>Visualizar</th>
              </tr>
            </thead>
            <tbody>
              {items.map((item) => (
                <tr key={item.id}>
                  <td>{documentLabels[item.tipo] ?? item.tipo}</td>
                  <td>{item.nomeArquivo || 'Documento'}</td>
                  <td>{item.contentType || '—'}</td>
                  <td>{formatBytes(item.tamanhoBytes)}</td>
                  <td>{date.format(new Date(item.enviadoEm))}</td>
                  <td>{item.status}</td>
                  <td>{item.motivoRejeicao ?? '—'}</td>
                  <td><button type="button" className="link-button" onClick={() => handleAbrir(item.arquivoUrl)}>Abrir</button></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
    </StorePage>
  )
}
