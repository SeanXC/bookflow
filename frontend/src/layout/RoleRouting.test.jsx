import { Route, Routes } from 'react-router-dom'
import { describe, expect, it, vi } from 'vitest'

import {
  renderWithProviders,
  screen,
} from '../test/testUtils.jsx'
import AppSidebar from './AppSidebar.jsx'
import ProtectedRoute from './ProtectedRoute.jsx'

function OwnerRouteTestApp() {
  return (
    <Routes>
      <Route element={<ProtectedRoute allowedRoles={['OWNER']} />}>
        <Route element={<div>Owner dashboard</div>} path="/dashboard" />
      </Route>
      <Route element={<div>Login page</div>} path="/login" />
      <Route element={<div>Role fallback</div>} path="/" />
    </Routes>
  )
}

describe('role routing', () => {
  it('redirects signed-out users to login', async () => {
    renderWithProviders(<OwnerRouteTestApp />, { route: '/dashboard' })

    expect(await screen.findByText('Login page')).toBeInTheDocument()
  })

  it('allows owners and redirects other roles from owner routes', async () => {
    const ownerSession = {
      accessToken: 'owner-token',
      user: { email: 'owner@example.com', role: 'OWNER' },
    }
    const ownerView = renderWithProviders(<OwnerRouteTestApp />, {
      route: '/dashboard',
      authSession: ownerSession,
    })
    expect(await screen.findByText('Owner dashboard')).toBeInTheDocument()
    ownerView.unmount()

    renderWithProviders(<OwnerRouteTestApp />, {
      route: '/dashboard',
      authSession: {
        accessToken: 'reception-token',
        user: { email: 'reception@example.com', role: 'RECEPTIONIST' },
      },
    })
    expect(await screen.findByText('Role fallback')).toBeInTheDocument()
  })

  it('shows only navigation permitted for staff', () => {
    renderWithProviders(
      <AppSidebar mobileOpen={false} onClose={vi.fn()} />,
      {
        route: '/appointments',
        authSession: {
          accessToken: 'staff-token',
          user: { email: 'staff@example.com', role: 'STAFF' },
        },
      },
    )

    expect(screen.getByRole('link', { name: 'My schedule' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Services' })).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Dashboard' })).not.toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Customers' })).not.toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Users' })).not.toBeInTheDocument()
  })
})
