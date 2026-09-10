import { ApiError } from '../api/apiError.js'

/**
 * @param {unknown} error
 * @returns {string}
 */
export function getAssistantErrorMessage(error) {
  if (error instanceof ApiError && error.code === 'BOOKING_CONFLICT') {
    return 'This time is no longer available. Please choose another slot.'
  }
  if (error instanceof ApiError && error.code === 'RATE_LIMITED') {
    return 'Too many assistant requests. Please try again later.'
  }
  return error instanceof Error
    ? error.message
    : 'The booking assistant is temporarily unavailable.'
}
