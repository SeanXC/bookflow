import { useMutation, useQueryClient } from '@tanstack/react-query'

import { createStaff, deactivateStaff, updateStaff } from './staffApi.js'
import { staffKeys } from './staffQueries.js'

/**
 * @param {{ staffId: number, request: import('../types.js').StaffRequest }} variables
 */
function updateStaffMutation({ staffId, request }) {
  return updateStaff(staffId, request)
}

export function useCreateStaff() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: createStaff,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: staffKeys.all }),
  })
}

export function useUpdateStaff() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: updateStaffMutation,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: staffKeys.all }),
  })
}

export function useDeactivateStaff() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: deactivateStaff,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: staffKeys.all }),
  })
}
