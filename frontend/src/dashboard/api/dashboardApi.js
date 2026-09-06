import { httpClient } from '../../api/httpClient.js'

/**
 * @returns {Promise<import('../types.js').DashboardSummary>}
 */
export async function getDashboardSummary() {
  const response = await httpClient.get('/api/dashboard/summary')
  return response.data
}
