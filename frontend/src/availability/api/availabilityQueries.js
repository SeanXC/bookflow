import { useQuery } from '@tanstack/react-query'

import {
  getAvailabilityExceptions,
  getAvailableSlots,
  getWeeklyHours,
} from './availabilityApi.js'

export const availabilityKeys = {
  all: ['availability'],
  /** @param {number} staffId */
  weeklyHours: (staffId) => [...availabilityKeys.all, 'weekly-hours', staffId],
  /**
   * @param {import('../types.js').AvailabilityDateRange} filters
   */
  exceptions: (filters) => [...availabilityKeys.all, 'exceptions', filters],
  /**
   * @param {import('../types.js').AvailableSlotFilters} filters
   */
  slots: (filters) => [...availabilityKeys.all, 'slots', filters],
}

/**
 * @param {number} staffId
 * @param {boolean} [enabled]
 */
export function useWeeklyHours(staffId, enabled = true) {
  return useQuery({
    enabled: enabled && Boolean(staffId),
    queryKey: availabilityKeys.weeklyHours(staffId),
    queryFn: () => getWeeklyHours(staffId),
  })
}

/**
 * @param {import('../types.js').AvailabilityDateRange} filters
 * @param {boolean} [enabled]
 */
export function useAvailabilityExceptions(filters, enabled = true) {
  return useQuery({
    enabled:
      enabled &&
      Boolean(filters.staffId) &&
      Boolean(filters.from) &&
      Boolean(filters.to),
    queryKey: availabilityKeys.exceptions(filters),
    queryFn: () => getAvailabilityExceptions(filters),
  })
}

/**
 * @param {import('../types.js').AvailableSlotFilters} filters
 * @param {boolean} [enabled]
 */
export function useAvailableSlots(filters, enabled = true) {
  return useQuery({
    enabled:
      enabled &&
      Boolean(filters.staffId) &&
      Boolean(filters.serviceId) &&
      Boolean(filters.from) &&
      Boolean(filters.to),
    queryKey: availabilityKeys.slots(filters),
    queryFn: () => getAvailableSlots(filters),
  })
}
