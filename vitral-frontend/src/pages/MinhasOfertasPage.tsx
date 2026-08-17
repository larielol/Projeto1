import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { StoreStatus } from '../components/Layout/StorePage'
import { Button } from '../components/ui/Button'
import { CurrencyInput } from '../components/ui/CurrencyInput'
import { DateTimeField } from '../components/ui/DateTimeField'
import { Input } from '../components/ui/Input'
import { Pagination } from '../components/ui/Pagination'
import { ofertaService } from '../services/ofertaService'
import { produtoService } from '../services/produtoService'
import { extractErrorMessage } from '../services/api'
import { seboService } from '../services/seboService'
import { useAuth } from '../hooks/useAuth'
import type { Produto } from '../types/produto'
import type { Oferta, OfertaRequest } from '../types/oferta'
import { StatusVerificacao, type StatusVerificacao as VerificationStatus } from '../types/sebo'
import './FormPage.css'
import './MeusProdutosPage.css'

type OfertaForm = {
  produtoId: number | ''
  precoPromocional: number
  descricao: string
  inicioEm: string
  fimEm: string
  ativa: boolean
}

const EMPTY: OfertaForm = {
  produtoId: '',
  precoPromocional: 0,
  descricao: '',
  inicioEm: '',
  fimEm: '',
  ativa: true,
}

function toInputValue(iso: string | null): string {
  if (!iso) return ''
  const date = new Date(iso)
  if (Number.isNaN(date.getTime())) return ''
  const pad = (value: number) => String(value).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`
    + `T${pad(date.getHours())}:${pad(date.getMinutes())}`
}

function toIsoOrUndefined(value: string): string | undefined {
  return value ? new Date(value).toISOString() : undefined
}

function formatarVigencia(oferta: Oferta): string {
  if (!oferta.inicioEm && !oferta.fimEm) return 'Sempre'
  const inicio = oferta.inicioEm ? new Date(oferta.inicioEm).toLocaleDateString('pt-BR') : '—'
  const fim = oferta.fimEm ? new Date(oferta.fimEm).toLocaleDateString('pt-BR') : '—'
  return `${inicio} até ${fim}`
}

export function MinhasOfertasPage() {
  const { seboId } = useAuth()
  const [ofertas, setOfertas] = useState<Oferta[]>([])
  const [produtos, setProdutos] = useState<Produto[]>([])
  const [loading, setLoading] = useState(true)
  const [form, setForm] = useState<OfertaForm>(EMPTY)
  const [editingId, setEditingId] = useState<number | null>(null)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [totalOfertas, setTotalOfertas] = useState(0)
  const [originalInicioEm, setOriginalInicioEm] = useState('')
  const [verification, setVerification] = useState<VerificationStatus | null>(null)

  const load = useCallback(() => {
    setLoading(true)
    ofertaService
      .listarMinhas(page, 10)
      .then((result) => {
        setOfertas(result.content)
        setTotalPages(result.totalPages)
        setTotalOfertas(result.totalElements)
      })
      .catch((err) => {
        setOfertas([])
        setError(extractErrorMessage(err, 'Erro ao listar ofertas'))
      })
      .finally(() => setLoading(false))
  }, [page])

  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    load()
  }, [load])

  useEffect(() => {
    if (!seboId) return
    let active = true
    produtoService
      .listarPorSebo(seboId, 0, 100)
      .then((result) => {
        if (active) setProdutos(result.content)
      })
      .catch(() => {
        if (active) setProdutos([])
      })

    return () => {
      active = false
    }
  }, [seboId])

  useEffect(() => {
    if (!seboId) return
    seboService.buscarPorId(seboId).then((item) => setVerification(item.statusVerificacao ?? null)).catch(() => setVerification(null))
  }, [seboId])

  function update<K extends keyof OfertaForm>(key: K, value: OfertaForm[K]) {
    setForm((prev) => ({ ...prev, [key]: value }))
  }

  if (verification && verification !== StatusVerificacao.VERIFICADO) {
    return <div className="page"><main className="container form-page"><StoreStatus title="Sebo ainda não verificado" description="As ofertas serão liberadas após a aprovação dos documentos." tone={verification === StatusVerificacao.REJEITADO ? 'error' : 'neutral'} action={<Link to="/painel/sebo/documentos">Gerenciar documentos</Link>} /></main></div>
  }

  function startEdit(oferta: Oferta) {
    setEditingId(oferta.id)
    const inicioEm = toInputValue(oferta.inicioEm)
    setForm({
      produtoId: oferta.produtoId,
      precoPromocional: oferta.precoPromocional,
      descricao: oferta.descricao ?? '',
      inicioEm,
      fimEm: toInputValue(oferta.fimEm),
      ativa: oferta.ativa,
    })
    setOriginalInicioEm(inicioEm)
    setError(null)
    setSuccess(null)
  }

  function resetForm() {
    setEditingId(null)
    setForm(EMPTY)
    setOriginalInicioEm('')
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setError(null)
    setSuccess(null)

    if (!form.produtoId) {
      setError('Selecione um produto para a oferta.')
      return
    }
    if (Number(form.precoPromocional) <= 0) {
      setError('O preço promocional deve ser maior que zero.')
      return
    }
    if (form.inicioEm && form.inicioEm !== originalInicioEm) {
      const inicio = new Date(form.inicioEm)
      const agora = new Date()
      agora.setSeconds(0, 0)
      if (inicio < agora) {
        setError('O início da oferta não pode ser no passado.')
        return
      }
    }
    if (form.inicioEm && form.fimEm && new Date(form.fimEm) < new Date(form.inicioEm)) {
      setError('O fim da oferta deve ser depois do início.')
      return
    }

    setBusy(true)
    try {
      const payload: OfertaRequest = {
        produtoId: Number(form.produtoId),
        precoPromocional: Number(form.precoPromocional),
        descricao: form.descricao || undefined,
        inicioEm: toIsoOrUndefined(form.inicioEm),
        fimEm: toIsoOrUndefined(form.fimEm),
        ativa: form.ativa,
      }
      if (editingId) {
        await ofertaService.atualizar(editingId, payload)
        setSuccess('Oferta atualizada!')
      } else {
        await ofertaService.criar(payload)
        setSuccess('Oferta criada!')
      }
      resetForm()
      load()
    } catch (err) {
      setError(extractErrorMessage(err, 'Erro ao salvar oferta'))
    } finally {
      setBusy(false)
    }
  }

  async function handleRemove(oferta: Oferta) {
    if (!confirm(`Desativar a oferta de "${oferta.tituloProduto}"?`)) return
    try {
      await ofertaService.remover(oferta.id)
      load()
    } catch (err) {
      setError(extractErrorMessage(err, 'Erro ao desativar oferta'))
    }
  }

  if (!seboId) {
    return (
      <div className="page">
        <main className="container form-page">
          <h1 className="form-page-title">Cadastre seu sebo primeiro</h1>
          <StoreStatus
            title="Perfil do sebo necessário"
            description="Você precisa criar o perfil do seu sebo antes de gerenciar ofertas."
            action={<Link to="/painel/sebo">Cadastrar sebo</Link>}
          />
        </main>
      </div>
    )
  }

  return (
    <div className="page">
      <main className="container form-page">
        <h1 className="form-page-title">Minhas ofertas</h1>
        {error ? <div className="flash error" role="alert">{error}</div> : null}
        {success ? <div className="flash success" role="status">{success}</div> : null}

        <form className="form-grid" onSubmit={handleSubmit}>
          <label className="vit-input-wrapper">
            <span className="vit-input-label">Produto</span>
            <select
              className="vit-input"
              value={form.produtoId}
              onChange={(e) => update('produtoId', e.target.value ? Number(e.target.value) : '')}
              required
            >
              <option value="">Selecione um produto</option>
              {produtos.map((produto) => (
                <option key={produto.id} value={produto.id}>
                  {produto.titulo} — R$ {Number(produto.preco).toFixed(2)}
                </option>
              ))}
            </select>
          </label>
          <CurrencyInput
            id="precoPromocional"
            label="Preço promocional (R$)"
            required
            value={form.precoPromocional}
            onChange={(value) => update('precoPromocional', value)}
          />
          <DateTimeField
            id="inicioEm"
            label="Início (opcional)"
            value={form.inicioEm}
            onChange={(value) => update('inicioEm', value)}
          />
          <DateTimeField
            id="fimEm"
            label="Fim (opcional)"
            value={form.fimEm}
            onChange={(value) => update('fimEm', value)}
          />
          <Input
            id="descricao"
            label="Descrição (opcional)"
            value={form.descricao}
            onChange={(e) => update('descricao', e.target.value)}
          />
          <label className="vit-input-wrapper vit-checkbox-wrapper">
            <input
              type="checkbox"
              checked={form.ativa}
              onChange={(e) => update('ativa', e.target.checked)}
            />
            <span className="vit-input-label">Oferta ativa</span>
          </label>
          <div className="form-actions">
            {editingId ? (
              <Button type="button" variant="ghost" onClick={resetForm}>Cancelar</Button>
            ) : null}
            <Button type="submit" disabled={busy}>
              {busy ? 'Salvando...' : editingId ? 'Atualizar oferta' : 'Criar oferta'}
            </Button>
          </div>
        </form>

        <h2 className="section-title">Ofertas cadastradas ({totalOfertas})</h2>
        {loading ? (
          <StoreStatus title="Carregando ofertas" description="Buscando as ofertas do seu sebo." busy />
        ) : ofertas.length === 0 ? (
          <StoreStatus
            title="Nenhuma oferta cadastrada"
            description="Crie a primeira promoção usando o formulário acima."
          />
        ) : (
          <>
            <div className="produto-table-scroll">
              <table className="produto-table">
                <thead>
                  <tr>
                    <th scope="col">Produto</th>
                    <th scope="col">Preço original</th>
                    <th scope="col">Preço promocional</th>
                    <th scope="col">Status</th>
                    <th scope="col">Vigência</th>
                    <th scope="col">Ações</th>
                  </tr>
                </thead>
                <tbody>
                  {ofertas.map((oferta) => (
                    <tr key={oferta.id}>
                      <td>{oferta.tituloProduto}</td>
                      <td>R$ {Number(oferta.precoOriginal).toFixed(2)}</td>
                      <td>R$ {Number(oferta.precoPromocional).toFixed(2)}</td>
                      <td>{oferta.ativa ? 'Ativa' : 'Inativa'}</td>
                      <td>{formatarVigencia(oferta)}</td>
                      <td className="produto-table-actions">
                        <Button type="button" variant="ghost" onClick={() => startEdit(oferta)}>Editar</Button>
                        {oferta.ativa ? (
                          <Button type="button" variant="danger" onClick={() => handleRemove(oferta)}>Desativar</Button>
                        ) : null}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
            <Pagination
              page={page}
              totalPages={totalPages}
              onPageChange={setPage}
              label="Páginas de ofertas"
            />
          </>
        )}
      </main>
    </div>
  )
}
