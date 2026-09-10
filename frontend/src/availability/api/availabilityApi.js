import { httpClient } from '../../api/httpClient.js'

/**
 * @param {number} staffId
 * @returns {Promise<import('../types.js').WeeklyHours[]>}
 */
export async function getWeeklyHours(staffId) {
  const response = await httpClient.get(
    `/api/staff/${staffId}/availability/weekly-hours`,
  )
  return response.data
}

/**
 * @param {number} staffId
 * @param {import('../types.js').WeeklyHoursRequest} request
 * @returns {Promise<import('../types.js').WeeklyHours>}
 */
export async function createWeeklyHours(staffId, request) {
  const response = await httpClient.post(
    `/api/staff/${staffId}/availability/weekly-hours`,
    request,
  )
  return response.data
}

/**
 * @param {number} staffId
 * @param {number} hoursId
 * @param {import('../types.js').WeeklyHoursRequest} request
 * @returns {Promise<import('../types.js').WeeklyHours>}
 */
export async function updateWeeklyHours(staffId, hoursId, request) {
  const response = await httpClient.put(
    `/api/staff/${staffId}/availability/weekly-hours/${hoursId}`,
    request,
  )
  return response.data
}

/**
 * @param {number} staffId
 * @param {number} hoursId
 * @returns {Promise<void>}
 */
export async function deleteWeeklyHours(staffId, hoursId) {
  await httpClient.delete(
    `/api/staff/${staffId}/availability/weekly-hours/${hoursId}`,
  )
}

/**
 * @param {import('../types.js').AvailabilityDateRange} filters
 * @returns {Promise<import('../types.js').AvailabilityException[]>}
 */
export async function getAvailabilityExceptions(filters) {
  const response = await httpClient.get(
    `/api/staff/${filters.staffId}/availability/exceptions`,
    {
      params: {
        from: filters.from,
        to: filters.to,
      },
    },
  )
  return response.data
}

/**
 * @param {number} staffId
 * @param {import('../types.js').AvailabilityExceptionRequest} request
 * @returns {Promise<import('../types.js').AvailabilityException>}
 */
export async function createAvailabilityException(staffId, request) {
  const response = await httpClient.post(
    `/api/staff/${staffId}/availability/exceptions`,
    request,
  )
  return response.data
}

/**
 * @param {number} staffId
 * @param {number} exceptionId
 * @param {import('../types.js').AvailabilityExceptionRequest} request
 * @returns {Promise<import('../types.js').AvailabilityException>}
 */
export async function updateAvailabilityException(
  staffId,
  exceptionId,
  request,
) {
  const response = await httpClient.put(
    `/api/staff/${staffId}/availability/exceptions/${exceptionId}`,
    request,
  )
  return response.data
}

/**
 * @param {number} staffId
 * @param {number} exceptionId
 * @returns {Promise<void>}
 */
export async function deleteAvailabilityException(staffId, exceptionId) {
  await httpClient.delete(
    `/api/staff/${staffId}/availability/exceptions/${exceptionId}`,
  )
}

/**
 * @param {import('../types.js').AvailableSlotFilters} filters
 * @returns {Promise<import('../types.js').AvailableSlot[]>}
 */
export async function getAvailableSlots(filters) {
  const response = await httpClient.get(
    `/api/staff/${filters.staffId}/availability/slots`,
    {
      params: {
        serviceId: filters.serviceId,
        from: filters.from,
        to: filters.to,
      },
    },
  )
  return response.data
}
