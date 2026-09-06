import axios from 'axios'
import { describe, expect, it } from 'vitest'

import { httpClient } from '../../api/httpClient.js'
import {
  renderWithProviders,
  screen,
  waitFor,
} from '../../test/testUtils.jsx'
import { useAuth } from './useAuth.js'

function AuthStateProbe() {
  const { isAuthenticated } = useAuth()
  return <div>{isAuthenticated ? 'Signed in' : 'Signed out'}</div>
}

describe('AuthProvider', () => {
  it('clears the session when the API returns 401', async () => {
    renderWithProviders(<AuthStateProbe />, {
      authSession: {
        accessToken: 'expired-token',
        user: {
          email: 'owner@example.com',
          role: 'OWNER',
        },
      },
    })

    expect(screen.getByText('Signed in')).toBeInTheDocument()

    const unauthorized = new axios.AxiosError(
      'Unauthorized',
      'ERR_BAD_REQUEST',
      {},
      null,
      {
        status: 401,
        statusText: 'Unauthorized',
        headers: {},
        config: {},
        data: {
          error: 'UNAUTHORIZED',
          message: 'Authentication is required.',
        },
      },
    )

    await expect(
      httpClient.get('/test-only', {
        adapter: async () => Promise.reject(unauthorized),
      }),
    ).rejects.toMatchObject({
      status: 401,
      code: 'UNAUTHORIZED',
    })

    await waitFor(() =>
      expect(screen.getByText('Signed out')).toBeInTheDocument(),
    )
  })
})
