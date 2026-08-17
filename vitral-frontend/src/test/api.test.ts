import { AxiosError, type AxiosResponse, type InternalAxiosRequestConfig } from 'axios'
import { afterEach, describe, expect, it } from 'vitest'
import { api, TOKEN_KEY } from '../services/api'

const originalAdapter = api.defaults.adapter

afterEach(() => {
  api.defaults.adapter = originalAdapter
})

describe('autenticacao da API', () => {
  it('envia o token salvo no header Authorization', async () => {
    localStorage.setItem(TOKEN_KEY, 'token-teste')
    let captured: InternalAxiosRequestConfig | undefined
    api.defaults.adapter = async (config) => {
      captured = config
      return response(config)
    }

    await api.get('/protegido')

    expect(captured?.headers.Authorization).toBe('Bearer token-teste')
  })

  it('limpa token, conta e sebo quando recebe 401', async () => {
    localStorage.setItem(TOKEN_KEY, 'expirado')
    localStorage.setItem('vitral_account', '{}')
    localStorage.setItem('vitral_sebo_id', '2')
    api.defaults.adapter = async (config) => {
      const result = response(config, 401)
      throw new AxiosError('Unauthorized', 'ERR_BAD_REQUEST', config, undefined, result)
    }

    await expect(api.get('/protegido')).rejects.toBeInstanceOf(AxiosError)

    expect(localStorage.getItem(TOKEN_KEY)).toBeNull()
    expect(localStorage.getItem('vitral_account')).toBeNull()
    expect(localStorage.getItem('vitral_sebo_id')).toBeNull()
  })

  it('encerra a sessao quando uma conta desativada recebe 403', async () => {
    localStorage.setItem(TOKEN_KEY, 'desativado')
    localStorage.setItem('vitral_account', '{"id":1}')
    localStorage.setItem('vitral_sebo_id', '3')
    api.defaults.adapter = async (config) => {
      const result = response(config, 403)
      throw new AxiosError('Forbidden', 'ERR_BAD_REQUEST', config, undefined, result)
    }

    await expect(api.get('/protegido')).rejects.toBeInstanceOf(AxiosError)

    expect(localStorage.getItem(TOKEN_KEY)).toBeNull()
    expect(localStorage.getItem('vitral_account')).toBeNull()
    expect(localStorage.getItem('vitral_sebo_id')).toBeNull()
  })
})

function response(config: InternalAxiosRequestConfig, status = 200): AxiosResponse {
  return {
    data: {},
    status,
    statusText: status === 200 ? 'OK' : 'Unauthorized',
    headers: {},
    config,
  }
}
