import { httpClient } from '../../api/httpClient.js'
import { toPageParams } from '../../api/pagination.js'

/**
 * @param {import('../types.js').AppointmentFilters} filters
 * @returns {Promise<import('../../api/pagination.js').PageResponse<
 *   import('../types.js').Appointment>>}
 */
export async function getAppointments(filters) {
  const response = await httpClient.get('/api/appointments', {
    params: {
      ...toPageParams(filters),
      ...(filters.staffId ? { staffId: filters.staffId } : {}),
      ...(filters.status ? { status: filters.status } : {}),
      ...(filters.from ? { from: filters.from } : {}),
      ...(filters.to ? { to: filters.to } : {}),
    },
  })
  return response.data
}

/**
 * @param {number} appointmentId
 * @returns {Promise<import('../types.js').Appointment>}
 */
export async function getAppointment(appointmentId) {
  const response = await httpClient.get(`/api/appointments/${appointmentId}`)
  return response.data
}

/**
 * @param {import('../types.js').AppointmentRequest} request
 * @returns {Promise<import('../types.js').Appointment>}
 */
export async function createAppointment(request) {
  const response = await httpClient.post('/api/appointments', request)
  return response.data
}

/**
 * @param {number} appointmentId
 * @param {import('../types.js').AppointmentRequest} request
 * @returns {Promise<import('../types.js').Appointment>}
 */
export async function updateAppointment(appointmentId, request) {
  const response = await httpClient.put(
    `/api/appointments/${appointmentId}`,
    request,
  )
  return response.data
}

/**
 * @param {number} appointmentId
 * @param {'COMPLETED' | 'CANCELLED'} status
 * @returns {Promise<import('../types.js').Appointment>}
 */
export async function updateAppointmentStatus(appointmentId, status) {
  const response = await httpClient.patch(
    `/api/appointments/${appointmentId}/status`,
    { status },
  )
  return response.data
}
