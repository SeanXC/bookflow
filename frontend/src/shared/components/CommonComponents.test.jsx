import { Route, Routes } from 'react-router-dom'
import { describe, expect, it } from 'vitest'

import HomeRedirect from '../../app/HomeRedirect.jsx'
import PublicBookingLayout from '../../publicbooking/components/PublicBookingLayout.jsx'
import { renderWithProviders, screen } from '../../test/testUtils.jsx'
import AppLoading from './AppLoading.jsx'
import PlaceholderPage from './PlaceholderPage.jsx'

describe('shared application components', () => {
  it('renders loading and placeholder states', () => {
    const { rerender } = renderWithProviders(<AppLoading />)
    expect(
      screen.getByRole('status', { name: 'Loading application' }),
    ).toBeInTheDocument()

    rerender(
      <PlaceholderPage
        description="Feature description"
        title="Feature title"
      />,
    )
    expect(
      screen.getByRole('heading', { name: 'Feature title' }),
    ).toBeInTheDocument()
    expect(screen.getByText('Feature description')).toBeInTheDocument()
  })

  it.each([
    ['OWNER', 'Owner home'],
    ['STAFF', 'Staff home'],
  ])('redirects %s users to their role home', async (role, destination) => {
    renderWithProviders(
      <Routes>
        <Route element={<HomeRedirect />} path="/" />
        <Route element={<div>Owner home</div>} path="/dashboard" />
        <Route element={<div>Staff home</div>} path="/appointments" />
      </Routes>,
      {
        authSession: {
          accessToken: 'token',
          user: { email: 'user@example.com', role },
        },
      },
    )

    expect(await screen.findByText(destination)).toBeInTheDocument()
  })

  it('renders public booking content inside its branded layout', () => {
    renderWithProviders(
      <Routes>
        <Route element={<PublicBookingLayout />}>
          <Route element={<div>Public booking content</div>} path="/book" />
        </Route>
      </Routes>,
      { route: '/book' },
    )

    expect(screen.getByText('BookFlow')).toBeInTheDocument()
    expect(screen.getByText('Public booking content')).toBeInTheDocument()
  })
})
