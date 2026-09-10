import { useMutation, useQueryClient } from '@tanstack/react-query'

import { createPublicAppointment } from './publicBookingApi.js'
import { publicBookingKeys } from './publicBookingQueries.js'

/**
 * @param {string} slug
 */
export function useCreatePublicAppointment(slug) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (
      /** @type {import('../types.js').PublicAppointmentRequest} */ request,
    ) => createPublicAppointment(slug, request),
    onSuccess: () =>
      queryClient.invalidateQueries({
        queryKey: publicBookingKeys.slotsRoot(slug),
      }),
  })
}
