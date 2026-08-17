import { useEffect, useRef, useState } from 'react'
import { Link, useLocation } from 'react-router-dom'
import { Button } from '../components/ui/Button'
import { ImageUploadField } from '../components/ui/ImageUploadField'
import { Input } from '../components/ui/Input'
import { usuarioService } from '../services/usuarioService'
import { cepService } from '../services/cepService'
import { extractErrorMessage } from '../services/api'
import { useAuthStore } from '../store/authStore'
import { AccountType, type Account, type UpdateProfileRequest } from '../types/account'
import { formatCep, formatCpf, isMaskedCpf, isValidCep, isValidCpf, isValidUf, onlyDigits } from '../utils/personalData'
import './FormPage.css'

type ProfileForm = {
  name: string
  fotoUrl: string
  cpf: string
  cep: string
  logradouro: string
  numero: string
  complemento: string
  bairro: string
  cidade: string
  estado: string
}

type CheckoutLocationState = { fromCheckout?: boolean; incompleteFields?: string[] }

const EMPTY: ProfileForm = { name: '', fotoUrl: '', cpf: '', cep: '', logradouro: '', numero: '', complemento: '', bairro: '', cidade: '', estado: '' }
const PURCHASE_FIELDS: Array<keyof ProfileForm> = ['cpf', 'cep', 'logradouro', 'numero', 'bairro', 'cidade', 'estado']

function accountToForm(account: Account): ProfileForm {
  const cpf = account.cpf ?? ''
  return {
    name: account.name ?? '',
    fotoUrl: account.fotoUrl ?? '',
    cpf: isMaskedCpf(cpf) ? cpf : formatCpf(cpf),
    cep: formatCep(account.cep ?? ''),
    logradouro: account.logradouro ?? '',
    numero: account.numero ?? '',
    complemento: account.complemento ?? '',
    bairro: account.bairro ?? '',
    cidade: account.cidade ?? '',
    estado: account.estado ?? '',
  }
}

function validateForPurchase(form: ProfileForm, requestedFields: string[]): Partial<Record<keyof ProfileForm, string>> {
  const errors: Partial<Record<keyof ProfileForm, string>> = {}
  const mustValidate = (field: keyof ProfileForm) => requestedFields.length === 0 || requestedFields.includes(field)
  if (mustValidate('cpf') && (isMaskedCpf(form.cpf) || !isValidCpf(form.cpf))) errors.cpf = 'Informe o CPF completo e válido.'
  if (mustValidate('cep') && !isValidCep(form.cep)) errors.cep = 'Informe um CEP válido com 8 dígitos.'
  if (mustValidate('logradouro') && !form.logradouro.trim()) errors.logradouro = 'Informe o logradouro.'
  if (mustValidate('numero') && !form.numero.trim()) errors.numero = 'Informe o número.'
  if (mustValidate('bairro') && !form.bairro.trim()) errors.bairro = 'Informe o bairro.'
  if (mustValidate('cidade') && !form.cidade.trim()) errors.cidade = 'Informe a cidade.'
  if (mustValidate('estado') && !isValidUf(form.estado)) errors.estado = 'Informe uma UF brasileira válida.'
  return errors
}

export function PerfilPage() {
  const location = useLocation()
  const account = useAuthStore((s) => s.account)
  const setAccount = useAuthStore((s) => s.setAccount)
  const locationState = location.state as CheckoutLocationState | null
  const fromCheckout = new URLSearchParams(location.search).get('from') === 'checkout' || locationState?.fromCheckout === true
  const isUsuario = account?.type === AccountType.USUARIO
  const [form, setForm] = useState<ProfileForm>(() => account ? accountToForm(account) : EMPTY)
  const [initialForm, setInitialForm] = useState<ProfileForm>(() => account ? accountToForm(account) : EMPTY)
  const [loading, setLoading] = useState(!account)
  const [busy, setBusy] = useState(false)
  const [erro, setErro] = useState<string | null>(null)
  const [fieldErrors, setFieldErrors] = useState<Partial<Record<keyof ProfileForm, string>>>(() => {
    const fields = locationState?.incompleteFields ?? []
    return Object.fromEntries(fields.filter((field): field is keyof ProfileForm => PURCHASE_FIELDS.includes(field as keyof ProfileForm)).map((field) => [field, 'Complete este campo para continuar a compra.']))
  })
  const [sucesso, setSucesso] = useState<string | null>(null)
  const [cepStatus, setCepStatus] = useState<'idle' | 'loading' | 'not-found'>('idle')
  const ultimoCepConsultado = useRef<string | null>(account ? onlyDigits(account.cep ?? '') : null)

  useEffect(() => {
    let active = true
    usuarioService.buscarPerfil().then((current) => {
      if (!active) return
      const currentForm = accountToForm(current)
      setAccount(current)
      setForm(currentForm)
      setInitialForm(currentForm)
      ultimoCepConsultado.current = onlyDigits(currentForm.cep)
      setErro(null)
    }).catch((err) => {
      if (active) setErro(extractErrorMessage(err, 'Falha ao carregar o perfil'))
    }).finally(() => {
      if (active) setLoading(false)
    })
    return () => { active = false }
  }, [setAccount])

  useEffect(() => {
    if (!isUsuario) return
    const digits = onlyDigits(form.cep)
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
          bairro: endereco.bairro || current.bairro,
          cidade: endereco.cidade || current.cidade,
          estado: endereco.uf || current.estado,
        }))
        setFieldErrors((current) => ({ ...current, logradouro: undefined, bairro: undefined, cidade: undefined, estado: undefined }))
        setCepStatus('idle')
      } else {
        setCepStatus('not-found')
      }
    })
    return () => { active = false }
  }, [form.cep, isUsuario])

  function update<K extends keyof ProfileForm>(key: K, value: ProfileForm[K]) {
    setForm((current) => ({ ...current, [key]: value }))
    setFieldErrors((current) => ({ ...current, [key]: undefined }))
  }

  function buildPayload(): UpdateProfileRequest {
    const payload: UpdateProfileRequest = { name: form.name.trim(), fotoUrl: form.fotoUrl.trim() }
    const optionalFields: Array<Exclude<keyof ProfileForm, 'name' | 'fotoUrl'>> = ['cpf', 'cep', 'logradouro', 'numero', 'complemento', 'bairro', 'cidade', 'estado']
    optionalFields.forEach((field) => {
      if (form[field] === initialForm[field]) return
      const value = form[field].trim()
      if (field === 'cpf') payload.cpf = onlyDigits(value)
      else if (field === 'cep') payload.cep = onlyDigits(value)
      else if (field === 'estado') payload.estado = value.toUpperCase()
      else payload[field] = value
    })
    return payload
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    setErro(null)
    setSucesso(null)
    const errors: Partial<Record<keyof ProfileForm, string>> = {}
    if (!form.name.trim()) errors.name = 'Informe seu nome.'
    if (isUsuario && form.cpf !== initialForm.cpf && form.cpf && !isValidCpf(form.cpf)) errors.cpf = 'Informe um CPF válido.'
    if (isUsuario && form.cep !== initialForm.cep && form.cep && !isValidCep(form.cep)) errors.cep = 'Informe um CEP válido com 8 dígitos.'
    if (isUsuario && form.estado !== initialForm.estado && form.estado && !isValidUf(form.estado)) errors.estado = 'Informe uma UF brasileira válida.'
    if (isUsuario && fromCheckout) Object.assign(errors, validateForPurchase(form, locationState?.incompleteFields ?? []))
    setFieldErrors(errors)
    if (Object.keys(errors).length > 0) {
      setErro(fromCheckout ? 'Complete os campos indicados para continuar a compra.' : 'Revise os campos indicados.')
      return
    }

    setBusy(true)
    try {
      const updated = await usuarioService.atualizarPerfil(buildPayload())
      const updatedForm = accountToForm(updated)
      setAccount(updated)
      setForm(updatedForm)
      setInitialForm(updatedForm)
      setSucesso('Perfil atualizado com sucesso.')
    } catch (err) {
      setErro(extractErrorMessage(err, 'Falha ao atualizar perfil'))
    } finally {
      setBusy(false)
    }
  }

  if (loading) return <div className="page"><main className="container form-page" aria-busy="true"><h1 className="form-page-title">Meu Perfil</h1><p>Carregando seus dados...</p></main></div>

  const maskedCpf = isMaskedCpf(form.cpf)

  return (
    <div className="page">
      <main className="container form-page">
        <h1 className="form-page-title">Meu Perfil</h1>
        {fromCheckout ? <div className="flash" role="status">Complete CPF e endereço para finalizar sua compra.</div> : null}
        {erro ? <div className="flash error" role="alert">{erro}</div> : null}
        {sucesso ? <div className="flash success" role="status">{sucesso} {fromCheckout ? <Link to="/confirmar-pedido">Voltar ao checkout</Link> : null}</div> : null}
        <form className="form-grid" onSubmit={handleSubmit} noValidate>
          <h2 className="form-section-title">Dados pessoais</h2>
          <div className="form-field-group">
            <Input id="perfil-name" label="Nome" value={form.name} onChange={(e) => update('name', e.target.value)} aria-invalid={Boolean(fieldErrors.name)} aria-describedby={fieldErrors.name ? 'perfil-name-error' : undefined} />
            {fieldErrors.name ? <small id="perfil-name-error" className="form-field-error">{fieldErrors.name}</small> : null}
          </div>
          <Input id="perfil-username" label="Username" value={account?.username ?? ''} disabled />
          <Input id="perfil-email" label="E-mail" type="email" value={account?.email ?? ''} disabled />
          <Input id="perfil-email-status" label="Situação do e-mail" value={account?.emailVerificado ? 'Verificado' : 'Não verificado'} disabled />
          {isUsuario ? (
            <div className="form-field-group">
              <Input id="perfil-cpf" label="CPF" inputMode="numeric" value={form.cpf} onFocus={() => { if (maskedCpf) update('cpf', '') }} onChange={(e) => update('cpf', formatCpf(e.target.value))} placeholder={maskedCpf ? 'Digite o CPF completo para alterá-lo' : '000.000.000-00'} maxLength={14} aria-invalid={Boolean(fieldErrors.cpf)} aria-describedby={fieldErrors.cpf ? 'perfil-cpf-error' : undefined} />
              {fieldErrors.cpf ? <small id="perfil-cpf-error" className="form-field-error">{fieldErrors.cpf}</small> : maskedCpf ? <small className="form-field-hint">CPF protegido. Para alterar, informe novamente os 11 dígitos.</small> : null}
            </div>
          ) : null}
          <ImageUploadField id="perfil-foto" label="Foto do perfil" value={form.fotoUrl} onChange={(value) => update('fotoUrl', value)} />

          {isUsuario ? (
            <>
              <h2 className="form-section-title">Endereço</h2>
              <div className="form-field-group">
                <Input id="perfil-cep" label="CEP" inputMode="numeric" value={form.cep} onChange={(e) => update('cep', formatCep(e.target.value))} maxLength={9} aria-invalid={Boolean(fieldErrors.cep)} aria-describedby={fieldErrors.cep ? 'perfil-cep-error' : undefined} />
                {fieldErrors.cep ? <small id="perfil-cep-error" className="form-field-error">{fieldErrors.cep}</small>
                  : cepStatus === 'loading' ? <small className="form-field-hint">Buscando endereço...</small>
                  : cepStatus === 'not-found' ? <small className="form-field-hint">CEP não encontrado. Preencha o endereço manualmente.</small>
                  : null}
              </div>
              <Input id="perfil-logradouro" label="Logradouro" value={form.logradouro} onChange={(e) => update('logradouro', e.target.value)} aria-invalid={Boolean(fieldErrors.logradouro)} />
              <Input id="perfil-numero" label="Número" value={form.numero} onChange={(e) => update('numero', e.target.value)} aria-invalid={Boolean(fieldErrors.numero)} />
              <Input id="perfil-complemento" label="Complemento (opcional)" value={form.complemento} onChange={(e) => update('complemento', e.target.value)} />
              <Input id="perfil-bairro" label="Bairro" value={form.bairro} onChange={(e) => update('bairro', e.target.value)} aria-invalid={Boolean(fieldErrors.bairro)} />
              <Input id="perfil-cidade" label="Cidade" value={form.cidade} onChange={(e) => update('cidade', e.target.value)} aria-invalid={Boolean(fieldErrors.cidade)} />
              <Input id="perfil-estado" label="Estado (UF)" value={form.estado} onChange={(e) => update('estado', e.target.value.toUpperCase().slice(0, 2))} minLength={2} maxLength={2} aria-invalid={Boolean(fieldErrors.estado)} />
              {PURCHASE_FIELDS.filter((field) => !['cpf', 'cep'].includes(field) && fieldErrors[field]).map((field) => <small key={field} className="form-field-error">{fieldErrors[field]}</small>)}
            </>
          ) : null}
          <div className="form-actions">
            <Button type="submit" disabled={busy}>{busy ? 'Salvando...' : 'Salvar alterações'}</Button>
            <Link to="/conta/excluir">Excluir conta</Link>
          </div>
        </form>
      </main>
    </div>
  )
}
