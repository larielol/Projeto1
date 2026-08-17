import { useEffect, useRef, useState } from 'react'
import { StoreStatus } from '../components/Layout/StorePage'
import { Button } from '../components/ui/Button'
import { ImageUploadField } from '../components/ui/ImageUploadField'
import { Input } from '../components/ui/Input'
import { seboService } from '../services/seboService'
import { pedidoService } from '../services/pedidoService'
import { cepService } from '../services/cepService'
import { extractErrorMessage } from '../services/api'
import { useAuth } from '../hooks/useAuth'
import { StatusConsultaCnpj, StatusVerificacao, type Sebo, type SeboRequest } from '../types/sebo'
import type { FaturamentoMensal } from '../types/pedido'
import { daysSince, formatDateBR, formatDateTimeBR } from '../utils/date'
import { onlyDigits } from '../utils/personalData'
import './FormPage.css'

const EMPTY: SeboRequest = {
  cnpj: '',
  descricao: '',
  telefone: '',
  fotoUrl: '',
  cep: '',
  logradouro: '',
  cidade: '',
  uf: '',
  horarioFuncionamento: '',
}

function formatCnpj(value: string) {
  const digits = value.replace(/\D/g, '').slice(0, 14)
  return digits
    .replace(/^(\d{2})(\d)/, '$1.$2')
    .replace(/^(\d{2})\.(\d{3})(\d)/, '$1.$2.$3')
    .replace(/^(\d{2})\.(\d{3})\.(\d{3})(\d)/, '$1.$2.$3/$4')
    .replace(/^(\d{2})\.(\d{3})\.(\d{3})\/(\d{4})(\d)/, '$1.$2.$3/$4-$5')
}

const MESES = ['Jan', 'Fev', 'Mar', 'Abr', 'Mai', 'Jun', 'Jul', 'Ago', 'Set', 'Out', 'Nov', 'Dez']
const formatBRL = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' })

export function MeuSeboPage() {
  const { setSeboId } = useAuth()
  const [form, setForm] = useState<SeboRequest>(EMPTY)
  const [existing, setExisting] = useState<Sebo | null>(null)
  const [loading, setLoading] = useState(true)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [success, setSuccess] = useState<string | null>(null)
  const [ano, setAno] = useState(new Date().getFullYear())
  const [mes, setMes] = useState(new Date().getMonth() + 1)
  const [faturamento, setFaturamento] = useState<FaturamentoMensal[]>([])
  const [loadingFaturamento, setLoadingFaturamento] = useState(true)
  const [faturamentoError, setFaturamentoError] = useState<string | null>(null)
  const [consultingCnpj, setConsultingCnpj] = useState(false)
  const [cepStatus, setCepStatus] = useState<'idle' | 'loading' | 'not-found'>('idle')
  const ultimoCepConsultado = useRef<string | null>(null)

  useEffect(() => {
    let active = true
    seboService.buscarMeu()
      .then((sebo) => {
        if (!active) return
        setExisting(sebo)
        setSeboId(sebo.id)
        setForm({
          cnpj: sebo.cnpj ?? '',
          descricao: sebo.descricao ?? '',
          telefone: sebo.telefone ?? '',
          fotoUrl: sebo.fotoUrl ?? '',
          cep: sebo.cep ?? '',
          logradouro: sebo.logradouro ?? '',
          cidade: sebo.cidade ?? '',
          uf: sebo.uf ?? '',
          horarioFuncionamento: sebo.horarioFuncionamento ?? '',
        })
        ultimoCepConsultado.current = onlyDigits(sebo.cep ?? '')
      })
      .catch(() => {
        // Ainda nao existe perfil cadastrado para esta conta (ou o backend esta indisponivel);
        // o formulario de cadastro em branco e o comportamento correto neste caso.
        if (active) setExisting(null)
      })
      .finally(() => {
        if (active) setLoading(false)
      })
    return () => {
      active = false
    }
  }, [setSeboId])

  useEffect(() => {
    let active = true
    // Reset the visible request state whenever the selected year changes.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    setLoadingFaturamento(true)
    setFaturamentoError(null)
    pedidoService.buscarFaturamentoMensal(ano)
      .then((data) => {
        if (active) setFaturamento(data)
      })
      .catch((err) => {
        if (!active) return
        setFaturamento([])
        setFaturamentoError(extractErrorMessage(err, 'Não foi possível carregar o faturamento.'))
      })
      .finally(() => {
        if (active) setLoadingFaturamento(false)
      })
    return () => { active = false }
  }, [ano])

  useEffect(() => {
    const digits = onlyDigits(form.cep ?? '')
    if (digits.length !== 8 || digits === ultimoCepConsultado.current) {
      setCepStatus('idle')
      return
    }
    let active = true
    setCepStatus('loading')
    cepService.buscar(digits).then((endereco) => {
      if (!active) return
      ultimoCepConsultado.current = digits
      if (endereco) {
        setForm((current) => ({
          ...current,
          logradouro: endereco.logradouro || current.logradouro,
          cidade: endereco.cidade || current.cidade,
          uf: endereco.uf || current.uf,
        }))
        setCepStatus('idle')
      } else {
        setCepStatus('not-found')
      }
    })
    return () => { active = false }
  }, [form.cep])

  function update<K extends keyof SeboRequest>(key: K, value: SeboRequest[K]) {
    setForm((prev) => ({ ...prev, [key]: value }))
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setError(null)
    setSuccess(null)
    if (!/^\d{14}$/.test(form.cnpj)) {
      setError('O CNPJ deve conter exatamente 14 números.')
      return
    }
    setBusy(true)
    try {
      const payload: SeboRequest = {
        ...form,
        cnpj: form.cnpj.replace(/\D/g, ''),
        cep: form.cep?.replace(/\D/g, ''),
        uf: form.uf?.toUpperCase(),
      }
      const saved = existing
        ? await seboService.atualizarMeu(payload)
        : await seboService.criar(payload)
      setExisting(saved)
      setSeboId(saved.id)
      setSuccess(existing ? 'Sebo atualizado com sucesso!' : 'Sebo cadastrado com sucesso!')
    } catch (err) {
      setError(extractErrorMessage(err, 'Erro ao salvar o sebo'))
    } finally {
      setBusy(false)
    }
  }

  const dataCriacao = formatDateBR(existing?.dataCriacao)
  const ultimaAtividade = formatDateTimeBR(existing?.ultimaAtividade)
  const diasSemAtividade = daysSince(existing?.ultimaAtividade)
  const activityTone = diasSemAtividade == null ? 'neutral' : diasSemAtividade > 90 ? 'inactive' : diasSemAtividade > 30 ? 'stale' : 'fresh'
  const activityText = diasSemAtividade == null
    ? 'Sem atividade registrada'
    : diasSemAtividade > 90
      ? 'Vendedor sem atividade há mais de 90 dias.'
      : diasSemAtividade > 30
        ? `Última atividade há ${diasSemAtividade} dias.`
        : ultimaAtividade

  async function consultarCnpj() {
    if (!existing) return
    setConsultingCnpj(true)
    setError(null)
    setSuccess(null)
    try {
      const result = await seboService.consultarMeuCnpj()
      setExisting((current) => current ? {
        ...current,
        cnpj: result.cnpj,
        razaoSocialReceita: result.razaoSocial,
        statusConsultaCnpj: result.status,
        cnpjConsultadoEm: result.consultadoEm,
        mensagemConsultaCnpj: result.mensagem,
      } : current)
      setSuccess('Consulta do CNPJ concluída.')
    } catch (err) {
      setError(extractErrorMessage(err, 'Não foi possível consultar o CNPJ.'))
    } finally {
      setConsultingCnpj(false)
    }
  }

  if (loading) {
    return (
      <div className="page">
        <main className="container form-page">
          <StoreStatus
            title="Carregando perfil do sebo"
            description="Buscando os dados do sebo vinculados à sua conta."
            busy
          />
        </main>
      </div>
    )
  }

  return (
    <div className="page">
      <main className="container form-page">
        <h1 className="form-page-title">{existing ? 'Editar perfil do sebo' : 'Cadastrar perfil do sebo'}</h1>
        {existing ? (
          <div className={`verification-status verification-${(existing.statusVerificacao ?? StatusVerificacao.PENDENTE).toLowerCase()}`} role="status">
            {existing.statusVerificacao === StatusVerificacao.VERIFICADO ? (
              <><strong>✓ Sebo verificado</strong><span>Seu sebo está habilitado para vender no Vitral.</span></>
            ) : existing.statusVerificacao === StatusVerificacao.REJEITADO ? (
              <><strong>Verificação recusada</strong><span>{existing.motivoRejeicao ? `Motivo: ${existing.motivoRejeicao}` : 'Revise o CNPJ e os documentos cadastrados antes de solicitar uma nova análise.'}</span></>
            ) : (
              <><strong>Cadastro em análise</strong><span>As ações comerciais serão liberadas após a verificação.</span></>
            )}
          </div>
        ) : null}
        <section className="verification-status" role="status" aria-live="polite">
          {existing ? (
            <>
              {dataCriacao ? <p><strong>Data de criação:</strong> {dataCriacao}</p> : null}
              <p className={`seller-activity seller-activity-${activityTone}`}><strong>Última atividade:</strong> {activityText}</p>
            </>
          ) : null}
          {existing?.confirmado ? <p><strong>✓ Sebo confirmado:</strong> este cadastro já passou pela verificação e foi validado.</p> : null}
        </section>
        {existing ? (
          <section className={`cnpj-consultation cnpj-${(existing.statusConsultaCnpj ?? StatusConsultaCnpj.NAO_CONSULTADO).toLowerCase()}`} aria-labelledby="cnpj-consultation-title">
            <div>
              <h2 id="cnpj-consultation-title">Consulta automática do CNPJ</h2>
              <p><strong>Status:</strong> {existing.statusConsultaCnpj ?? StatusConsultaCnpj.NAO_CONSULTADO}</p>
              {existing.razaoSocialReceita ? <p><strong>Razão social na Receita:</strong> {existing.razaoSocialReceita}</p> : null}
              {existing.cnpjConsultadoEm ? <p><strong>Última consulta:</strong> {new Date(existing.cnpjConsultadoEm).toLocaleString('pt-BR')}</p> : null}
              {existing.mensagemConsultaCnpj ? <p>{existing.mensagemConsultaCnpj}</p> : null}
              {existing.statusConsultaCnpj === StatusConsultaCnpj.ATIVA ? <p className="cnpj-guidance">CNPJ ativo. Confira se a razão social corresponde aos documentos enviados.</p> : null}
              {existing.statusConsultaCnpj === StatusConsultaCnpj.INATIVA ? <p className="cnpj-guidance">O CNPJ consta como inativo e não poderá ser aprovado.</p> : null}
              {existing.statusConsultaCnpj === StatusConsultaCnpj.INDISPONIVEL ? <p className="cnpj-guidance">A consulta externa está indisponível. Tente novamente mais tarde.</p> : null}
            </div>
            <Button type="button" variant="ghost" disabled={consultingCnpj} onClick={consultarCnpj}>
              {consultingCnpj ? 'Consultando...' : 'Consultar CNPJ'}
            </Button>
          </section>
        ) : null}
        {error ? <div className="flash error" role="alert">{error}</div> : null}
        {success ? <div className="flash success" role="status">{success}</div> : null}
        <form className="form-grid" onSubmit={handleSubmit}>
          <Input id="cnpj" label="CNPJ" required inputMode="numeric" maxLength={18}
            value={formatCnpj(form.cnpj)}
            onChange={(e) => update('cnpj', e.target.value.replace(/\D/g, '').slice(0, 14))}
            pattern="\d{2}\.\d{3}\.\d{3}/\d{4}-\d{2}"
            title="Informe os 14 números do CNPJ" />
          <Input id="descricao" label="Descrição"
            value={form.descricao ?? ''} onChange={(e) => update('descricao', e.target.value)} />
          <Input id="telefone" label="Telefone"
            value={form.telefone ?? ''} onChange={(e) => update('telefone', e.target.value)} />
          <div className="form-field-group">
            <Input id="cep" label="CEP" inputMode="numeric" maxLength={9} required
              value={form.cep ?? ''}
              onChange={(e) => update('cep', e.target.value.replace(/\D/g, '').slice(0, 8))}
              placeholder="Somente números" />
            {cepStatus === 'loading' ? <small className="form-field-hint">Buscando endereço...</small>
              : cepStatus === 'not-found' ? <small className="form-field-hint">CEP não encontrado. Preencha o endereço manualmente.</small>
              : null}
          </div>
          <Input id="logradouro" label="Endereço/logradouro" required
            value={form.logradouro ?? ''} onChange={(e) => update('logradouro', e.target.value)} />
          <Input id="cidade" label="Cidade" required
            value={form.cidade ?? ''} onChange={(e) => update('cidade', e.target.value)} />
          <Input id="uf" label="UF" maxLength={2} required
            value={form.uf ?? ''} onChange={(e) => update('uf', e.target.value.toUpperCase().slice(0, 2))} />
          <Input id="horarioFuncionamento" label="Horário de funcionamento (opcional)"
            value={form.horarioFuncionamento ?? ''} onChange={(e) => update('horarioFuncionamento', e.target.value)} />
          <ImageUploadField
            id="fotoUrl"
            label="Foto do sebo"
            value={form.fotoUrl}
            onChange={(url) => update('fotoUrl', url)}
          />
          <div className="form-actions">
            <Button type="submit" disabled={busy}>
              {busy ? 'Salvando...' : existing ? 'Atualizar' : 'Cadastrar'}
            </Button>
          </div>
        </form>

        <section className="billing-section" aria-labelledby="billing-title">
          <div className="billing-heading">
            <div>
              <h2 id="billing-title">Faturamento mensal</h2>
              <p>Acompanhe as vendas confirmadas do seu sebo.</p>
            </div>
            <label>
              Ano
              <select value={ano} onChange={(e) => setAno(Number(e.target.value))}>
                {Array.from({ length: 5 }, (_, index) => new Date().getFullYear() - index).map((value) => (
                  <option key={value} value={value}>{value}</option>
                ))}
              </select>
            </label>
            <label>
              Mês
              <select value={mes} onChange={(e) => setMes(Number(e.target.value))}>
                {MESES.map((label, index) => <option key={label} value={index + 1}>{label}</option>)}
              </select>
            </label>
          </div>
          {loadingFaturamento ? (
            <StoreStatus title="Carregando faturamento" description="Consultando os valores mensais." busy />
          ) : faturamentoError ? (
            <div className="flash error" role="alert">{faturamentoError}</div>
          ) : (() => {
            const relatorio = MESES.map((_, index) => (
              faturamento.find((item) => item.mes === index + 1) ?? {
                ano,
                mes: index + 1,
                vendasOnline: 0,
                reembolsos: 0,
                total: 0,
              }
            ))
            const valores = relatorio.map((item) => Number(item.total))
            const totalOnline = relatorio.reduce((sum, item) => sum + Number(item.vendasOnline), 0)
            const totalReembolsos = relatorio.reduce((sum, item) => sum + Number(item.reembolsos), 0)
            const total = relatorio.reduce((sum, item) => sum + Number(item.total), 0)
            const selecionado = relatorio[mes - 1]
            const max = Math.max(...valores, 1)
            return (
              <>
                <p className="billing-total">Total em {ano}: <strong>{formatBRL.format(total)}</strong></p>
                <div className="billing-summary-grid" aria-label={`Resumo financeiro de ${ano}`}>
                  <span>Vendas online <strong>{formatBRL.format(totalOnline)}</strong></span>
                  <span>Reembolsos <strong>{formatBRL.format(totalReembolsos)}</strong></span>
                </div>
                <div className="billing-month-card" aria-label={`Detalhamento de ${MESES[mes - 1]} de ${ano}`}>
                  <h3>{MESES[mes - 1]}/{ano}</h3>
                  <dl>
                    <div><dt>Vendas online</dt><dd>{formatBRL.format(selecionado.vendasOnline)}</dd></div>
                    <div><dt>Reembolsos</dt><dd>{formatBRL.format(selecionado.reembolsos)}</dd></div>
                    <div><dt>Total</dt><dd>{formatBRL.format(selecionado.total)}</dd></div>
                  </dl>
                </div>
                {faturamento.length === 0 ? <p className="billing-empty">Nenhum faturamento registrado neste ano.</p> : null}
                <div className="billing-chart" aria-label={`Faturamento mensal de ${ano}`}>
                  {valores.map((value, index) => (
                    <div className="billing-bar-column" key={MESES[index]}>
                      <span className="billing-value">{formatBRL.format(value)}</span>
                      <span className="billing-bar" style={{ height: `${Math.max((value / max) * 140, value ? 4 : 0)}px` }} />
                      <span>{MESES[index]}</span>
                    </div>
                  ))}
                </div>
                <div className="billing-table-scroll">
                  <table className="billing-table">
                    <thead><tr><th>Mês</th><th>Online</th><th>Reembolsos</th><th>Total</th></tr></thead>
                    <tbody>{relatorio.map((item, index) => (
                      <tr key={MESES[index]}>
                        <td>{MESES[index]}</td>
                        <td>{formatBRL.format(item.vendasOnline)}</td>
                        <td>{formatBRL.format(item.reembolsos)}</td>
                        <td>{formatBRL.format(item.total)}</td>
                      </tr>
                    ))}</tbody>
                  </table>
                </div>
              </>
            )
          })()}
        </section>
      </main>
    </div>
  )
}
