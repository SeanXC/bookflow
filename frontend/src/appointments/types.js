/**
 * @typedef {'CONFIRMED' | 'COMPLETED' | 'CANCELLED'} AppointmentStatus
 */

/**
 * @typedef {object} Appointment
 * @property {number} id
 * @property {number} customerId
 * @property {number} staffId
 * @property {number} serviceId
 * @property {{ id: number, firstName: string, lastName: string,
 *   email: string | null, phone: string | null }} customer
 * @property {{ id: number, firstName: string, lastName: string }} staff
 * @property {{ id: number, name: string, price: number,
 *   durationMinutes: number }} service
 * @property {string} startTime
 * @property {string} endTime
 * @property {AppointmentStatus} status
 * @property {string | null} notes
 * @property {string} createdAt
 * @property {string} updatedAt
 */

/**
 * @typedef {object} AppointmentRequest
 * @property {number} customerId
 * @property {number} staffId
 * @property {number} serviceId
 * @property {string} startTime ISO-8601 UTC instant
 * @property {string | null} notes
 */

/**
 * @typedef {object} AppointmentFilters
 * @property {number} [staffId]
 * @property {AppointmentStatus} [status]
 * @property {string} [from] ISO-8601 UTC instant
 * @property {string} [to] ISO-8601 UTC instant
 * @property {number} page
 * @property {number} pageSize
 * @property {Array<{ field: string, sort?: 'asc' | 'desc' | null }>} sortModel
 */

export {}
