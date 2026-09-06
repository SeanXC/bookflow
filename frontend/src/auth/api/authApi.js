import { httpClient } from '../../api/httpClient.js'

/**
 * @param {import('../types.js').LoginRequest} request
 * @returns {Promise<import('../types.js').AuthSession>}
 */
export async function login(request) {
  const response = await httpClient.post('/api/auth/login', request)
  return response.data
}

/**
 * @param {import('../types.js').RegisterRequest} request
 * @returns {Promise<import('../types.js').AuthSession>}
 */
export async function register(request) {
  const response = await httpClient.post('/api/auth/register', request)
  return response.data
}
