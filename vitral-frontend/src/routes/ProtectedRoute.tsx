import type { ReactNode } from 'react'
import { Navigate } from 'react-router-dom'
import { useAuth } from '../hooks/useAuth'
import type { AccountType } from '../types/account'

type Props = {
  children: ReactNode
  requireRole?: AccountType
}

export function ProtectedRoute({ children, requireRole }: Props) {
  const { isAuthenticated, account } = useAuth()

  if (!isAuthenticated) {
    return <Navigate to="/auth" replace />
  }
  if (requireRole && account?.type !== requireRole) {
    return <Navigate to="/" replace />
  }
  return <>{children}</>
}
