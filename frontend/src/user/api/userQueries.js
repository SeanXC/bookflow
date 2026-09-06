import { keepPreviousData, useQuery } from '@tanstack/react-query'

import { getManagedUsers, getUsers } from './userApi.js'

export const userKeys = {
  all: ['users'],
  /** @param {Parameters<typeof getUsers>[0]} options */
  list: (options) => [...userKeys.all, 'list', options],
}

/**
 * @param {Parameters<typeof getUsers>[0]} options
 */
export function useUsers(options) {
  return useQuery({
    queryKey: userKeys.list(options),
    queryFn: () => getUsers(options),
    placeholderData: keepPreviousData,
  })
}

/** @param {boolean} enabled */
export function useManagedUsers(enabled) {
  return useQuery({
    enabled,
    queryKey: [...userKeys.all, 'managed-options'],
    queryFn: getManagedUsers,
    staleTime: 60_000,
  })
}
