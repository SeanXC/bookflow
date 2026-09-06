import { useMutation, useQueryClient } from '@tanstack/react-query'

import {
  createAppointment,
  updateAppointment,
  updateAppointmentStatus,
} from './appointmentApi.js'
import { appointmentKeys } from './appointmentQueries.js'

/**
 * @param {{
 *   appointmentId: number,
 *   request: import('../types.js').AppointmentRequest
 * }} variables
 */
function updateAppointmentMutation({ appointmentId, request }) {
  return updateAppointment(appointmentId, request)
}

/**
 * @param {{
 *   appointmentId: number,
 *   status: 'COMPLETED' | 'CANCELLED'
 * }} variables
 */
function updateStatusMutation({ appointmentId, status }) {
  return updateAppointmentStatus(appointmentId, status)
}

function useAppointmentMutationOptions() {
  const queryClient = useQueryClient()

  return {
    onSuccess: (appointment) => {
      queryClient.setQueryData(
        appointmentKeys.detail(appointment.id),
        appointment,
      )
      return queryClient.invalidateQueries({
        queryKey: appointmentKeys.lists(),
      })
    },
  }
}

export function useCreateAppointment() {
  return useMutation({
    mutationFn: createAppointment,
    ...useAppointmentMutationOptions(),
  })
}

export function useUpdateAppointment() {
  return useMutation({
    mutationFn: updateAppointmentMutation,
    ...useAppointmentMutationOptions(),
  })
}

export function useUpdateAppointmentStatus() {
  return useMutation({
    mutationFn: updateStatusMutation,
    ...useAppointmentMutationOptions(),
  })
}
