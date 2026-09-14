import { Route, Routes } from 'react-router-dom'
import { describe, expect, it } from 'vitest'

import {
  renderWithProviders,
  screen,
  waitFor,
} from '../test/testUtils.jsx'
import AppLayout from './AppLayout.jsx'

const session = {
  accessToken: 'owner-token',
  user: { email: 'owner@example.com', role: 'OWNER' },
}

function LayoutRoutes() {
  return (
    <Routes>
      <Route element={<AppLayout />}>
        <Route element={<div>Dashboard content</div>} path="/dashboard" />
        <Route element={<div>Customer content</div>} path="/customers" />
      </Route>
      <Route element={<div>Login screen</div>} path="/login" />
    </Routes>
  )
}

describe('AppLayout', () => {
  it('renders account details, outlet content, and opens mobile navigation', async () => {
    const { user } = renderWithProviders(<LayoutRoutes />, {
      route: '/dashboard',
      authSession: session,
    })

    expect(screen.getByText('Dashboard content')).toBeInTheDocument()
    expect(screen.getByText('owner@example.com')).toBeInTheDocument()
    expect(screen.getByText('OWNER')).toBeInTheDocument()

    await user.click(
      screen.getByRole('button', { name: 'Open navigation' }),
    )
    await waitFor(() =>
      expect(screen.getAllByText('BookFlow')).toHaveLength(2),
    )
    await user.click(screen.getAllByRole('link', { name: 'Customers' })[0])
    expect(await screen.findByText('Customer content')).toBeInTheDocument()
  })

  it('clears the session and navigates to login when signing out', async () => {
    const { user } = renderWithProviders(<LayoutRoutes />, {
      route: '/dashboard',
      authSession: session,
    })

    await user.click(screen.getByRole('button', { name: 'Sign out' }))

    expect(await screen.findByText('Login screen')).toBeInTheDocument()
  })
})
