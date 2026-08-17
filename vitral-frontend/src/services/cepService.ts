import { onlyDigits } from '../utils/personalData'

export type EnderecoPorCep = {
  logradouro: string
  bairro: string
  cidade: string
  uf: string
}

export const cepService = {
  async buscar(cep: string): Promise<EnderecoPorCep | null> {
    const digits = onlyDigits(cep)
    if (digits.length !== 8) return null
    try {
      const response = await fetch(`https://viacep.com.br/ws/${digits}/json/`, {
        signal: AbortSignal.timeout(8000),
      })
      if (!response.ok) return null
      const data = await response.json()
      if (data.erro) return null
      return {
        logradouro: data.logradouro ?? '',
        bairro: data.bairro ?? '',
        cidade: data.localidade ?? '',
        uf: data.uf ?? '',
      }
    } catch {
      return null
    }
  },
}
