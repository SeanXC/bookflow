import { ApiError } from '../api/apiError.js'

/**
 * @param {unknown} error
 * @returns {string}
 */
export function getPublicBookingErrorMessage(error) {
  if (error instanceof ApiError && error.code === 'BOOKING_CONFLICT') {
    return 'This time is no longer available. Please choose another slot.'
  }
  if (error instanceof ApiError && error.code === 'RATE_LIMITED') {
    return 'Too many booking attempts. Please try again later.'
  }
  return error instanceof Error
    ? error.message
    : 'Unable to complete this booking.'
}
