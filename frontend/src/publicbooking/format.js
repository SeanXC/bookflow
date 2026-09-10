import { MAX_SLOT_RANGE_DAYS } from './constants.js'

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
 * @param {string} instant
 * @returns {string}
 */
export function formatSlotTime(instant) {
  return new Date(instant).toLocaleTimeString('en-IE', {
    hour: '2-digit',
    minute: '2-digit',
  })
}

/**
 * @param {string} instant
 * @returns {string}
 */
export function formatSlotDate(instant) {
  return new Date(instant).toLocaleDateString('en-IE', {
    dateStyle: 'full',
  })
}

/**
 * @param {string} startTime
 * @param {string} endTime
 * @returns {string}
 */
export function formatSlotRange(startTime, endTime) {
  return `${formatSlotDate(startTime)} · ${formatSlotTime(startTime)} – ${formatSlotTime(endTime)}`
}

/**
 * @param {string} from
 * @param {string} to
 * @returns {string | null}
 */
export function getSlotRangeError(from, to) {
  if (!from || !to) {
    return 'Choose a start and end date.'
  }
  if (from > to) {
    return 'The start date must not be after the end date.'
  }
  const start = new Date(`${from}T00:00:00`)
  const end = new Date(`${to}T00:00:00`)
  const days = Math.round((end.getTime() - start.getTime()) / 86_400_000) + 1
  if (days > MAX_SLOT_RANGE_DAYS) {
    return `The date range cannot exceed ${MAX_SLOT_RANGE_DAYS} days.`
  }
  return null
}

/**
 * @returns {{ from: string, to: string }}
 */
export function defaultSlotRange() {
  const fromDate = new Date()
  const toDate = new Date()
  toDate.setDate(toDate.getDate() + (MAX_SLOT_RANGE_DAYS - 1))
  return {
    from: formatLocalDate(fromDate),
    to: formatLocalDate(toDate),
  }
}

/**
 * @param {import('./types.js').PublicAvailableSlot[]} slots
 * @returns {Array<[string, import('./types.js').PublicAvailableSlot[]]>}
 */
export function groupSlotsByDay(slots) {
  /** @type {Map<string, import('./types.js').PublicAvailableSlot[]>} */
  const groups = new Map()
  for (const slot of slots) {
    const key = formatLocalDate(new Date(slot.startTime))
    const existing = groups.get(key)
    if (existing) {
      existing.push(slot)
    } else {
      groups.set(key, [slot])
    }
  }
  return [...groups.entries()]
}
