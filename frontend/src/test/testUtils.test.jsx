import { Typography } from '@mui/material'
import { describe, expect, it } from 'vitest'

import { useAuth } from '../auth/context/useAuth.js'
import { renderWithProviders, screen } from './testUtils.jsx'

function AuthStateProbe() {
  const { isAuthenticated, user } = useAuth()
  return (
    <Typography>
      {isAuthenticated ? `${user.role}: ${user.email}` : 'Signed out'}
    </Typography>
  )
}

describe('renderWithProviders', () => {
  it('hydrates the auth provider with the supplied session', () => {
    renderWithProviders(<AuthStateProbe />, {
      authSession: {
        accessToken: 'test-token',
        user: {
          email: 'owner@example.com',
          role: 'OWNER',
        },
      },
    })

    expect(screen.getByText('OWNER: owner@example.com')).toBeInTheDocument()
  })
})
