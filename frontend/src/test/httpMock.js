import { vi } from 'vitest'

import { ApiError } from '../api/apiError.js'
import { httpClient } from '../api/httpClient.js'

/**
 * Spies on one Axios method. Tests can use mockResolvedValueOnce with
 * apiResponse() or mockRejectedValueOnce with apiError().
 *
 * @param {'get' | 'post' | 'put' | 'patch'} method
 */
export function mockHttp(method) {
  return vi.spyOn(httpClient, method)
}

/**
 * @template T
 * @param {T} data
 */
export function apiResponse(data) {
  return { data }
}

/**
 * @param {string} message
 * @param {{ status?: number, code?: string }} [options]
 */
export function apiError(message, options = {}) {
  return new ApiError(message, options)
}
