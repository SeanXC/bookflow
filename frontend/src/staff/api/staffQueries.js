import { keepPreviousData, useQuery } from '@tanstack/react-query'

import { getStaff } from './staffApi.js'

export const staffKeys = {
  all: ['staff'],
  /** @param {Parameters<typeof getStaff>[0]} filters */
  list: (filters) => [...staffKeys.all, 'list', filters],
}

/**
 * @param {Parameters<typeof getStaff>[0]} options
 * @param {boolean} [enabled]
 */
export function useStaff(options, enabled = true) {
  return useQuery({
    enabled,
    queryKey: staffKeys.list(options),
    queryFn: () => getStaff(options),
    placeholderData: keepPreviousData,
  })
}
