import { keepPreviousData, useQuery } from '@tanstack/react-query'

import { getServices } from './serviceApi.js'

export const serviceKeys = {
  all: ['services'],
  /** @param {Parameters<typeof getServices>[0]} filters */
  list: (filters) => [...serviceKeys.all, 'list', filters],
}

/**
 * @param {Parameters<typeof getServices>[0]} options
 */
export function useServices(options) {
  return useQuery({
    queryKey: serviceKeys.list(options),
    queryFn: () => getServices(options),
    placeholderData: keepPreviousData,
  })
}
