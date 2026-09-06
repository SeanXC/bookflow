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
