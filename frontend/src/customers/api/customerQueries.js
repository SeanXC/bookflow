import { keepPreviousData, useQuery } from '@tanstack/react-query'

import { getCustomers } from './customerApi.js'

export const customerKeys = {
  all: ['customers'],
  /** @param {Parameters<typeof getCustomers>[0]} filters */
  list: (filters) => [...customerKeys.all, 'list', filters],
}

/**
 * @param {Parameters<typeof getCustomers>[0]} options
 */
export function useCustomers(options) {
  return useQuery({
    queryKey: customerKeys.list(options),
    queryFn: () => getCustomers(options),
    placeholderData: keepPreviousData,
  })
}
