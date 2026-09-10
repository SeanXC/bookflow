/** Inclusive maximum for exception date filters, matching the backend. */
export const MAX_EXCEPTION_RANGE_DAYS = 31

export const DAYS_OF_WEEK = [
  { value: 'MONDAY', label: 'Monday' },
  { value: 'TUESDAY', label: 'Tuesday' },
  { value: 'WEDNESDAY', label: 'Wednesday' },
  { value: 'THURSDAY', label: 'Thursday' },
  { value: 'FRIDAY', label: 'Friday' },
  { value: 'SATURDAY', label: 'Saturday' },
  { value: 'SUNDAY', label: 'Sunday' },
]

export const DAY_ORDER = Object.fromEntries(
  DAYS_OF_WEEK.map((day, index) => [day.value, index]),
)

export const EXCEPTION_TYPES = [
  { value: 'UNAVAILABLE', label: 'Unavailable' },
  { value: 'CUSTOM_HOURS', label: 'Custom hours' },
]
