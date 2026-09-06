import { httpClient } from '../../api/httpClient.js'
import { toPageParams } from '../../api/pagination.js'

/**
 * @param {{
 *   search?: string,
 *   active?: boolean,
 *   page: number,
 *   pageSize: number,
 *   sortModel: Array<{ field: string, sort?: 'asc' | 'desc' | null }>
 * }} options
 * @returns {Promise<import('../../api/pagination.js').PageResponse<
 *   import('../types.js').ServiceItem>>}
 */
export async function getServices(options) {
  const pageParams = toPageParams(options)
  const response = await httpClient.get('/api/services', {
    params: {
      ...pageParams,
      ...(options.search ? { search: options.search } : {}),
      ...(typeof options.active === 'boolean'
        ? { active: options.active }
        : {}),
    },
  })
  return response.data
}

/**
 * @param {import('../types.js').ServiceRequest} request
 * @returns {Promise<import('../types.js').ServiceItem>}
 */
export async function createService(request) {
  const response = await httpClient.post('/api/services', request)
  return response.data
}

/**
 * @param {number} serviceId
 * @param {import('../types.js').ServiceRequest} request
 * @returns {Promise<import('../types.js').ServiceItem>}
 */
export async function updateService(serviceId, request) {
  const response = await httpClient.put(`/api/services/${serviceId}`, request)
  return response.data
}

/**
 * @param {number} serviceId
 * @returns {Promise<void>}
 */
export async function deactivateService(serviceId) {
  await httpClient.delete(`/api/services/${serviceId}`)
}
