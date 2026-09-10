import {
  DAYS_OF_WEEK,
  EXCEPTION_TYPES,
  MAX_EXCEPTION_RANGE_DAYS,
} from './constants.js'

/**
 * @param {Date} date
 * @returns {string}
 */
export function formatLocalDate(date) {
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  return `${year}-${month}-${day}`
}

/**
 * @param {string} value
 * @returns {string}
 */
export function toTimeInputValue(value) {
  return value ? value.slice(0, 5) : ''
}

/**
 * @param {string} value
 * @returns {string}
 */
export function toApiTime(value) {
  if (!value) {
    return ''
  }
  return value.length === 5 ? `${value}:00` : value
}

/**
 * @param {string | null} value
 * @returns {string}
 */
export function formatTime(value) {
  return value ? toTimeInputValue(value) : '—'
}

/**
 * @param {string} dayOfWeek
 * @returns {string}
 */
export function formatDayOfWeek(dayOfWeek) {
  return DAYS_OF_WEEK.find((day) => day.value === dayOfWeek)?.label ?? dayOfWeek
}

/**
 * @param {string} type
 * @returns {string}
 */
export function formatExceptionType(type) {
  return EXCEPTION_TYPES.find((item) => item.value === type)?.label ?? type
}

/**
 * @param {string} from
 * @param {string} to
 * @returns {string | null}
 */
export function getExceptionRangeError(from, to) {
  if (!from || !to) {
    return 'Choose a start and end date.'
  }
  if (from > to) {
    return 'The start date must not be after the end date.'
  }
  const start = new Date(`${from}T00:00:00`)
  const end = new Date(`${to}T00:00:00`)
  const days = Math.round((end.getTime() - start.getTime()) / 86_400_000) + 1
  if (days > MAX_EXCEPTION_RANGE_DAYS) {
    return `The date range cannot exceed ${MAX_EXCEPTION_RANGE_DAYS} days.`
  }
  return null
}
