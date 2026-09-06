import { httpClient } from '../../api/httpClient.js'
import { toPageParams } from '../../api/pagination.js'

/**
 * @param {{
 *   page: number,
 *   pageSize: number,
 *   sortModel: Array<{ field: string, sort?: 'asc' | 'desc' | null }>
 * }} options
 * @returns {Promise<import('../../api/pagination.js').PageResponse<
 *   import('../types.js').ManagedUser>>}
 */
export async function getUsers(options) {
  const response = await httpClient.get('/api/users', {
    params: toPageParams(options),
  })
  return response.data
}

/**
 * Returns the account choices used by the staff-linking form.
 */
export function getManagedUsers() {
  return getUsers({
    page: 0,
    pageSize: 100,
    sortModel: [{ field: 'email', sort: 'asc' }],
  })
}
