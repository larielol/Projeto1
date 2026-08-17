export type Page<T> = {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
  size: number
  first: boolean
  last: boolean
}

export type ApiErrorField = {
  field: string
  message: string
}

export type ApiError = {
  timestamp: string
  status: number
  error: string
  code?: string | null
  message: string
  path: string
  fieldErrors: ApiErrorField[]
}
