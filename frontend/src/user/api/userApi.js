import { httpClient } from '../../api/httpClient.js'

/**
 * @returns {Promise<import('../../api/pagination.js').PageResponse<
 *   import('../types.js').ManagedUser>>}
 */
export async function getManagedUsers() {
  const response = await httpClient.get('/api/users', {
    params: {
      page: 0,
      size: 100,
      sort: 'email,asc',
    },
  })
  return response.data
}
