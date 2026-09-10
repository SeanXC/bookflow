import { keepPreviousData, useQuery } from '@tanstack/react-query'

import { getStaff, getStaffById } from './staffApi.js'

export const staffKeys = {
  all: ['staff'],
  /** @param {Parameters<typeof getStaff>[0]} filters */
  list: (filters) => [...staffKeys.all, 'list', filters],
  /** @param {number} staffId */
  detail: (staffId) => [...staffKeys.all, 'detail', staffId],
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

/**
 * @param {number} staffId
 * @param {boolean} [enabled]
 */
export function useStaffById(staffId, enabled = true) {
  return useQuery({
    enabled: enabled && Boolean(staffId),
    queryKey: staffKeys.detail(staffId),
    queryFn: () => getStaffById(staffId),
  })
}
