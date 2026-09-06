import { QueryClient } from '@tanstack/react-query'

import { ApiError } from './apiError.js'

export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      refetchOnWindowFocus: false,
      retry: (failureCount, error) =>
        failureCount < 1 &&
        (!(error instanceof ApiError) ||
          error.status === 0 ||
          error.status >= 500),
      staleTime: 30_000,
    },
    mutations: {
      retry: false,
    },
  },
})
