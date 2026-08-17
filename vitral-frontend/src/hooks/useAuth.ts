import { useAuthStore } from '../store/authStore'

export function useAuth() {
  const token = useAuthStore((s) => s.token)
  const account = useAuthStore((s) => s.account)
  const seboId = useAuthStore((s) => s.seboId)
  const login = useAuthStore((s) => s.login)
  const logout = useAuthStore((s) => s.logout)
  const setSeboId = useAuthStore((s) => s.setSeboId)
  const sessionMessage = useAuthStore((s) => s.sessionMessage)
  const clearSessionMessage = useAuthStore((s) => s.clearSessionMessage)

  return {
    token,
    account,
    seboId,
    isAuthenticated: Boolean(token),
    isSebo: account?.type === 'SEBO',
    isUsuario: account?.type === 'USUARIO',
    isAdmin: account?.type === 'ADMIN',
    login,
    logout,
    setSeboId,
    sessionMessage,
    clearSessionMessage,
  }
}
