import { useMutation, useQueryClient } from '@tanstack/react-query'

import { createUser, updateUserEnabled } from './userApi.js'
import { userKeys } from './userQueries.js'

/**
 * @param {{ userId: number, enabled: boolean }} variables
 */
function updateEnabledMutation({ userId, enabled }) {
  return updateUserEnabled(userId, enabled)
}

function useUserMutationOptions() {
  const queryClient = useQueryClient()
  return {
    onSuccess: () =>
      queryClient.invalidateQueries({
        queryKey: userKeys.all,
      }),
  }
}

export function useCreateUser() {
  return useMutation({
    mutationFn: createUser,
    ...useUserMutationOptions(),
  })
}

export function useUpdateUserEnabled() {
  return useMutation({
    mutationFn: updateEnabledMutation,
    ...useUserMutationOptions(),
  })
}
