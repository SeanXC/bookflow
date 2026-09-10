/**
 * @typedef {'MONDAY' | 'TUESDAY' | 'WEDNESDAY' | 'THURSDAY' | 'FRIDAY'
 *   | 'SATURDAY' | 'SUNDAY'} DayOfWeek
 */

/**
 * @typedef {'UNAVAILABLE' | 'CUSTOM_HOURS'} AvailabilityExceptionType
 */

/**
 * @typedef {object} WeeklyHours
 * @property {number} id
 * @property {number} staffId
 * @property {DayOfWeek} dayOfWeek
 * @property {string} startTime
 * @property {string} endTime
 */

/**
 * @typedef {object} WeeklyHoursRequest
 * @property {DayOfWeek} dayOfWeek
 * @property {string} startTime
 * @property {string} endTime
 */

/**
 * @typedef {object} AvailabilityException
 * @property {number} id
 * @property {number} staffId
 * @property {string} exceptionDate
 * @property {AvailabilityExceptionType} type
 * @property {string | null} startTime
 * @property {string | null} endTime
 * @property {string | null} note
 */

/**
 * @typedef {object} AvailabilityExceptionRequest
 * @property {string} exceptionDate
 * @property {AvailabilityExceptionType} type
 * @property {string | null} [startTime]
 * @property {string | null} [endTime]
 * @property {string | null} [note]
 */

/**
 * @typedef {object} AvailableSlot
 * @property {string} startTime
 * @property {string} endTime
 */

/**
 * @typedef {object} AvailabilityDateRange
 * @property {number} staffId
 * @property {string} from
 * @property {string} to
 */

/**
 * @typedef {object} AvailableSlotFilters
 * @property {number} staffId
 * @property {number} serviceId
 * @property {string} from
 * @property {string} to
 */

export {}
