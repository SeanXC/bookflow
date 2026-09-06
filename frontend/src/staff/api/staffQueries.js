import { keepPreviousData, useQuery } from '@tanstack/react-query'

import { getStaff } from './staffApi.js'

export const staffKeys = {
  all: ['staff'],
  /** @param {Parameters<typeof getStaff>[0]} filters */
  list: (filters) => [...staffKeys.all, 'list', filters],
}

/**
 * @param {Parameters<typeof getStaff>[0]} options
 */
export function useStaff(options) {
  return useQuery({
    queryKey: staffKeys.list(options),
    queryFn: () => getStaff(options),
    placeholderData: keepPreviousData,
  })
}
