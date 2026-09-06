import { Suspense } from 'react'
import { QueryClientProvider } from '@tanstack/react-query'
import { CssBaseline, ThemeProvider } from '@mui/material'
import { RouterProvider } from 'react-router-dom'

import { queryClient } from '../api/queryClient.js'
import AuthProvider from '../auth/context/AuthProvider.jsx'
import AppLoading from '../shared/components/AppLoading.jsx'
import { router } from './router.jsx'
import { theme } from './theme.js'

function AppProviders() {
  return (
    <ThemeProvider theme={theme}>
      <CssBaseline />
      <QueryClientProvider client={queryClient}>
        <AuthProvider>
          <Suspense fallback={<AppLoading />}>
            <RouterProvider router={router} />
          </Suspense>
        </AuthProvider>
      </QueryClientProvider>
    </ThemeProvider>
  )
}

export default AppProviders
