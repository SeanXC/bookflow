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
