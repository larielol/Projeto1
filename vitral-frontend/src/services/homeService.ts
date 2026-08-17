import { api } from './api'
import type { HomeResponse } from '../types/home'

export const homeService = {
  async carregar(): Promise<HomeResponse> {
    const { data } = await api.get<HomeResponse>('/home')
    return data
  },
}
