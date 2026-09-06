import { ApiError } from '../api/apiError.js'

/**
 * @param {unknown} error
 * @returns {error is ApiError}
 */
export function isBookingConflict(error) {
  return error instanceof ApiError && error.code === 'BOOKING_CONFLICT'
}

/**
 * Returns an appointment-specific message while preserving other API errors.
 *
 * @param {unknown} error
 * @returns {string}
 */
export function getAppointmentErrorMessage(error) {
  if (isBookingConflict(error)) {
    return 'This staff member already has an overlapping appointment.'
  }

  return error instanceof Error
    ? error.message
    : 'Unable to save the appointment.'
}
