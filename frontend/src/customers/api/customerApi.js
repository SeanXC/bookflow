import { httpClient } from '../../api/httpClient.js'
import { toPageParams } from '../../api/pagination.js'

/**
 * @param {{
 *   search?: string,
 *   page: number,
 *   pageSize: number,
 *   sortModel: Array<{ field: string, sort?: 'asc' | 'desc' | null }>
 * }} options
 * @returns {Promise<import('../../api/pagination.js').PageResponse<
 *   import('../types.js').Customer>>}
 */
export async function getCustomers(options) {
  const response = await httpClient.get('/api/customers', {
    params: {
      ...toPageParams(options),
      ...(options.search ? { search: options.search } : {}),
    },
  })
  return response.data
}

/**
 * @param {number} customerId
 * @returns {Promise<import('../types.js').Customer>}
 */
export async function getCustomer(customerId) {
  const response = await httpClient.get(`/api/customers/${customerId}`)
  return response.data
}

/**
 * @param {number} customerId
 * @param {{ page: number, pageSize: number }} options
 * @returns {Promise<import('../../api/pagination.js').PageResponse<
 *   import('../../appointments/types.js').Appointment>>}
 */
export async function getCustomerAppointments(customerId, options) {
  const response = await httpClient.get(
    `/api/customers/${customerId}/appointments`,
    {
      params: {
        page: options.page,
        size: options.pageSize,
      },
    },
  )
  return response.data
}

/**
 * @param {import('../types.js').CustomerRequest} request
 * @returns {Promise<import('../types.js').Customer>}
 */
export async function createCustomer(request) {
  const response = await httpClient.post('/api/customers', request)
  return response.data
}

/**
 * @param {number} customerId
 * @param {import('../types.js').CustomerRequest} request
 * @returns {Promise<import('../types.js').Customer>}
 */
export async function updateCustomer(customerId, request) {
  const response = await httpClient.put(
    `/api/customers/${customerId}`,
    request,
  )
  return response.data
}
