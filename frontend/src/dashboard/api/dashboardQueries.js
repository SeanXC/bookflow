import { useQuery } from '@tanstack/react-query'

import { getDashboardSummary } from './dashboardApi.js'

export const dashboardKeys = {
  all: ['dashboard'],
  summary: () => [...dashboardKeys.all, 'summary'],
}

export function useDashboardSummary() {
  return useQuery({
    queryKey: dashboardKeys.summary(),
    queryFn: getDashboardSummary,
  })
}
