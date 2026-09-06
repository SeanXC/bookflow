import { keepPreviousData, useQuery } from '@tanstack/react-query'

import { getAppointment, getAppointments } from './appointmentApi.js'

export const appointmentKeys = {
  all: ['appointments'],
  lists: () => [...appointmentKeys.all, 'list'],
  /** @param {import('../types.js').AppointmentFilters} filters */
  list: (filters) => [...appointmentKeys.lists(), filters],
  details: () => [...appointmentKeys.all, 'detail'],
  /** @param {number} appointmentId */
  detail: (appointmentId) => [...appointmentKeys.details(), appointmentId],
}

/**
 * @param {import('../types.js').AppointmentFilters} filters
 */
export function useAppointments(filters) {
  return useQuery({
    queryKey: appointmentKeys.list(filters),
    queryFn: () => getAppointments(filters),
    placeholderData: keepPreviousData,
  })
}

/**
 * @param {number} appointmentId
 * @param {boolean} [enabled]
 */
export function useAppointment(appointmentId, enabled = true) {
  return useQuery({
    enabled,
    queryKey: appointmentKeys.detail(appointmentId),
    queryFn: () => getAppointment(appointmentId),
  })
}
