import { useMutation, useQueryClient } from '@tanstack/react-query'

import {
  createService,
  deactivateService,
  updateService,
} from './serviceApi.js'
import { serviceKeys } from './serviceQueries.js'

/**
 * @param {{
 *   serviceId: number,
 *   request: import('../types.js').ServiceRequest
 * }} variables
 */
function updateServiceMutation({ serviceId, request }) {
  return updateService(serviceId, request)
}

export function useCreateService() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: createService,
    onSuccess: () =>
      queryClient.invalidateQueries({ queryKey: serviceKeys.all }),
  })
}

export function useUpdateService() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: updateServiceMutation,
    onSuccess: () =>
      queryClient.invalidateQueries({ queryKey: serviceKeys.all }),
  })
}

export function useDeactivateService() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: deactivateService,
    onSuccess: () =>
      queryClient.invalidateQueries({ queryKey: serviceKeys.all }),
  })
}
