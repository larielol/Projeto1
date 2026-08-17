import { api } from './api'

export type UploadResponse = {
  url: string
}

export const uploadService = {
  async uploadImage(file: File): Promise<UploadResponse> {
    const formData = new FormData()
    formData.append('file', file)
    const { data } = await api.post<UploadResponse>('/uploads/images', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    })
    return data
  },
}
