import { useQuery } from '@tanstack/react-query'

import { getManagedUsers } from './userApi.js'

export const userKeys = {
  all: ['users'],
}

/** @param {boolean} enabled */
export function useManagedUsers(enabled) {
  return useQuery({
    enabled,
    queryKey: userKeys.all,
    queryFn: getManagedUsers,
    staleTime: 60_000,
  })
}
