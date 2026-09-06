import axios from 'axios'

import {
  AUTH_UNAUTHORIZED_EVENT,
  clearAuthSession,
  getAccessToken,
} from '../auth/storage/authStorage.js'
import { toApiError } from './apiError.js'

const apiBaseUrl = (
  import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080'
).replace(/\/+$/, '')

export const httpClient = axios.create({
  baseURL: apiBaseUrl,
  headers: {
    Accept: 'application/json',
  },
  paramsSerializer: {
    indexes: null,
  },
  timeout: 10_000,
})

httpClient.interceptors.request.use((config) => {
  const accessToken = getAccessToken()
  if (accessToken) {
    config.headers.Authorization = `Bearer ${accessToken}`
  }
  return config
})

httpClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (axios.isAxiosError(error) && error.response?.status === 401) {
      clearAuthSession()
      window.dispatchEvent(new Event(AUTH_UNAUTHORIZED_EVENT))
    }
    return Promise.reject(toApiError(error))
  },
)
