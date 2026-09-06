import axios from 'axios'

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

httpClient.interceptors.response.use(
  (response) => response,
  (error) => Promise.reject(toApiError(error)),
)
