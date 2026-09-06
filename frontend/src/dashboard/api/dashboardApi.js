import { httpClient } from '../../api/httpClient.js'

/**
 * @returns {Promise<import('../types.js').DashboardSummary>}
 */
export async function getDashboardSummary() {
  const response = await httpClient.get('/api/dashboard/summary')
  return response.data
}

/**
 * @returns {Promise<import('../types.js').DailyBooking[]>}
 */
export async function getBookingsByWeek() {
  const response = await httpClient.get('/api/dashboard/bookings-by-week')
  return response.data
}

/**
 * @returns {Promise<import('../types.js').MonthlyRevenue[]>}
 */
export async function getRevenueByMonth() {
  const response = await httpClient.get('/api/dashboard/revenue-by-month')
  return response.data
}
