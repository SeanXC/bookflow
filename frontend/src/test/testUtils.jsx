/* eslint-disable react-refresh/only-export-components */
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { CssBaseline, ThemeProvider } from '@mui/material'
import { render as testingLibraryRender } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import PropTypes from 'prop-types'
import { MemoryRouter } from 'react-router-dom'

import { theme } from '../app/theme.js'
import AuthProvider from '../auth/context/AuthProvider.jsx'
import {
  clearAuthSession,
  writeAuthSession,
} from '../auth/storage/authStorage.js'

export * from '@testing-library/react'
export { userEvent }

export function createTestQueryClient() {
  return new QueryClient({
    defaultOptions: {
      queries: {
        gcTime: Infinity,
        retry: false,
      },
      mutations: {
        retry: false,
      },
    },
  })
}

/**
 * @param {import('react').ReactNode} ui
 * @param {{
 *   route?: string,
 *   queryClient?: QueryClient,
 *   authSession?: import('../auth/types.js').AuthSession | null
 * }} [options]
 */
export function renderWithProviders(ui, options = {}) {
  const {
    route = '/',
    queryClient = createTestQueryClient(),
    authSession = null,
  } = options

  if (authSession) {
    writeAuthSession(authSession)
  } else {
    clearAuthSession()
  }

  function Wrapper({ children }) {
    return (
      <ThemeProvider theme={theme}>
        <CssBaseline />
        <QueryClientProvider client={queryClient}>
          <AuthProvider>
            <MemoryRouter initialEntries={[route]}>{children}</MemoryRouter>
          </AuthProvider>
        </QueryClientProvider>
      </ThemeProvider>
    )
  }

  Wrapper.propTypes = {
    children: PropTypes.node.isRequired,
  }

  return {
    user: userEvent.setup(),
    queryClient,
    ...testingLibraryRender(ui, { wrapper: Wrapper }),
  }
}
