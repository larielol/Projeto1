import axios, { AxiosError } from 'axios'
import type { ApiError } from '../types/common'

export const TOKEN_KEY = 'vitral_token'
export const PROFILE_INCOMPLETE_FOR_PURCHASE = 'PROFILE_INCOMPLETE_FOR_PURCHASE'

export type ProfileIncompleteError = {
  message: string
  fields: Array<{ field: string; message: string }>
}

export const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '/api/v1',
  headers: { 'Content-Type': 'application/json' },
})

api.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

api.interceptors.response.use(
  (response) => response,
  async (error: AxiosError<ApiError>) => {
    if (error.response?.status === 401 || error.response?.status === 403) {
      const hadSession = Boolean(localStorage.getItem(TOKEN_KEY))
      const { useAuthStore } = await import('../store/authStore')
      useAuthStore.getState().forceLogout(
        hadSession ? 'Sua sessão expirou. Entre novamente para continuar.' : undefined,
      )
    }
    return Promise.reject(error)
  },
)

export function extractErrorMessage(error: unknown, fallback = 'Erro inesperado'): string {
  if (error instanceof AxiosError) {
    const apiError = error.response?.data as ApiError | undefined
    if (apiError?.fieldErrors?.length) {
      return apiError.fieldErrors.map((f) => `${f.field}: ${f.message}`).join(' | ')
    }
    if (apiError?.message) return apiError.message
    if (error.message) return error.message
  }
  return fallback
}

export function extractProfileIncompleteError(error: unknown): ProfileIncompleteError | null {
  if (!(error instanceof AxiosError) || error.response?.status !== 422) return null
  const apiError = error.response.data as ApiError | undefined
  if (apiError?.code !== PROFILE_INCOMPLETE_FOR_PURCHASE) return null
  return {
    message: apiError.message || 'Complete CPF e endereço antes de finalizar a compra.',
    fields: apiError.fieldErrors ?? [],
  }
}
