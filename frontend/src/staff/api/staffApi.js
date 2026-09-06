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
 *   import('../types.js').StaffMember>>}
 */
export async function getStaff(options) {
  const pageParams = toPageParams(options)
  const response = await httpClient.get('/api/staff', {
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
 * @param {import('../types.js').StaffRequest} request
 * @returns {Promise<import('../types.js').StaffMember>}
 */
export async function createStaff(request) {
  const response = await httpClient.post('/api/staff', request)
  return response.data
}

/**
 * @param {number} staffId
 * @param {import('../types.js').StaffRequest} request
 * @returns {Promise<import('../types.js').StaffMember>}
 */
export async function updateStaff(staffId, request) {
  const response = await httpClient.put(`/api/staff/${staffId}`, request)
  return response.data
}

/**
 * @param {number} staffId
 * @returns {Promise<void>}
 */
export async function deactivateStaff(staffId) {
  await httpClient.delete(`/api/staff/${staffId}`)
}
