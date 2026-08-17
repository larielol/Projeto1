import { create } from 'zustand'
import type { Account } from '../types/account'
import { api, TOKEN_KEY } from '../services/api'

const ACCOUNT_KEY = 'vitral_account'
const SEBO_ID_KEY = 'vitral_sebo_id'

function readAccount(): Account | null {
  const raw = localStorage.getItem(ACCOUNT_KEY)
  if (!raw) return null
  try {
    return JSON.parse(raw) as Account
  } catch {
    return null
  }
}

function readSeboId(): number | null {
  const raw = localStorage.getItem(SEBO_ID_KEY)
  if (!raw) return null
  const parsed = Number(raw)
  return Number.isFinite(parsed) ? parsed : null
}

function clearStoredSession() {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(ACCOUNT_KEY)
  localStorage.removeItem(SEBO_ID_KEY)
}

type AuthState = {
  token: string | null
  account: Account | null
  seboId: number | null
  sessionMessage: string | null
  login: (token: string, account: Account) => void
  logout: () => void
  forceLogout: (message?: string) => void
  clearSessionMessage: () => void
  setAccount: (account: Account) => void
  setSeboId: (seboId: number) => void
}

export const useAuthStore = create<AuthState>((set) => ({
  token: localStorage.getItem(TOKEN_KEY),
  account: readAccount(),
  seboId: readSeboId(),
  sessionMessage: null,

  login: (token, account) => {
    localStorage.setItem(TOKEN_KEY, token)
    localStorage.setItem(ACCOUNT_KEY, JSON.stringify(account))
    localStorage.removeItem(SEBO_ID_KEY)
    set({ token, account, seboId: null, sessionMessage: null })
  },

  logout: () => {
    api.post('/auth/logout').catch(() => {})
    clearStoredSession()
    set({ token: null, account: null, seboId: null, sessionMessage: null })
  },

  forceLogout: (message) => {
    clearStoredSession()
    set({ token: null, account: null, seboId: null, sessionMessage: message ?? null })
  },

  clearSessionMessage: () => set({ sessionMessage: null }),

  setAccount: (account) => {
    localStorage.setItem(ACCOUNT_KEY, JSON.stringify(account))
    set({ account })
  },

  setSeboId: (seboId) => {
    localStorage.setItem(SEBO_ID_KEY, String(seboId))
    set({ seboId })
  },
}))
