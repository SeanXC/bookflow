import axios from 'axios'

/**
 * Normalized error exposed to the UI.
 */
export class ApiError extends Error {
  /**
   * @param {string} message
   * @param {{
   *   status?: number,
   *   code?: string,
   *   timestamp?: string | null,
   *   cause?: unknown
   * }} [options]
   */
  constructor(message, options = {}) {
    super(message, { cause: options.cause })
    this.name = 'ApiError'
    this.status = options.status ?? 0
    this.code = options.code ?? 'UNKNOWN_ERROR'
    this.timestamp = options.timestamp ?? null
  }
}

/**
 * Converts Axios, network, and unexpected failures to one stable shape.
 *
 * @param {unknown} error
 * @returns {ApiError}
 */
export function toApiError(error) {
  if (error instanceof ApiError) {
    return error
  }

  if (axios.isAxiosError(error)) {
    const payload = error.response?.data
    const status = error.response?.status ?? 0
    const message =
      typeof payload?.message === 'string'
        ? payload.message
        : status === 0
          ? 'Unable to connect to the server.'
          : `Request failed with status ${status}.`

    return new ApiError(message, {
      status,
      code:
        typeof payload?.error === 'string'
          ? payload.error
          : status === 0
            ? 'NETWORK_ERROR'
            : 'HTTP_ERROR',
      timestamp:
        typeof payload?.timestamp === 'string' ? payload.timestamp : null,
      cause: error,
    })
  }

  return new ApiError(
    error instanceof Error ? error.message : 'An unexpected error occurred.',
    { cause: error },
  )
}
