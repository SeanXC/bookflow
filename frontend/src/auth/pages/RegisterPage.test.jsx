import { Route, Routes } from 'react-router-dom'
import { describe, expect, it } from 'vitest'

import { apiError, apiResponse, mockHttp } from '../../test/httpMock.js'
import {
  renderWithProviders,
  screen,
  waitFor,
} from '../../test/testUtils.jsx'
import RegisterPage from './RegisterPage.jsx'

function RegisterTestRoutes() {
  return (
    <Routes>
      <Route element={<RegisterPage />} path="/register" />
      <Route element={<div>Authenticated home</div>} path="/" />
    </Routes>
  )
}

const authResponse = {
  accessToken: 'owner-token',
  user: { email: 'owner@example.com', role: 'OWNER' },
}

describe('RegisterPage', () => {
  it('does not submit while required fields are empty', async () => {
    const post = mockHttp('post')
    const { user } = renderWithProviders(<RegisterTestRoutes />, {
      route: '/register',
    })

    await user.click(screen.getByRole('button', { name: 'Create account' }))

    expect(screen.getByLabelText(/^Business name/)).toBeInvalid()
    expect(screen.getByLabelText(/^Business email/)).toBeInvalid()
    expect(screen.getByLabelText(/^Owner email/)).toBeInvalid()
    expect(screen.getByLabelText(/^Password/)).toBeInvalid()
    expect(post).not.toHaveBeenCalled()
  })

  it('normalizes registration details and navigates home', async () => {
    const post = mockHttp('post').mockResolvedValueOnce(apiResponse(authResponse))
    const { user } = renderWithProviders(<RegisterTestRoutes />, {
      route: '/register',
    })

    await user.type(screen.getByLabelText(/^Business name/), '  Glow Studio  ')
    await user.type(
      screen.getByLabelText(/^Business email/),
      '  HELLO@EXAMPLE.COM  ',
    )
    await user.type(
      screen.getByLabelText('Business phone (optional)'),
      '  +353123456  ',
    )
    await user.type(
      screen.getByLabelText(/^Owner email/),
      '  OWNER@EXAMPLE.COM  ',
    )
    await user.type(screen.getByLabelText(/^Password/), 'DemoPass123!')
    await user.click(screen.getByRole('button', { name: 'Create account' }))

    await waitFor(() =>
      expect(post).toHaveBeenCalledWith('/api/auth/register', {
        businessName: 'Glow Studio',
        businessEmail: 'hello@example.com',
        businessPhone: '+353123456',
        ownerEmail: 'owner@example.com',
        password: 'DemoPass123!',
      }),
    )
    expect(await screen.findByText('Authenticated home')).toBeInTheDocument()
  })

  it('omits a blank optional business phone', async () => {
    const post = mockHttp('post').mockResolvedValueOnce(apiResponse(authResponse))
    const { user } = renderWithProviders(<RegisterTestRoutes />, {
      route: '/register',
    })

    await user.type(screen.getByLabelText(/^Business name/), 'Glow Studio')
    await user.type(
      screen.getByLabelText(/^Business email/),
      'hello@example.com',
    )
    await user.type(screen.getByLabelText(/^Owner email/), 'owner@example.com')
    await user.type(screen.getByLabelText(/^Password/), 'DemoPass123!')
    await user.click(screen.getByRole('button', { name: 'Create account' }))

    await waitFor(() =>
      expect(post).toHaveBeenCalledWith('/api/auth/register', {
        businessName: 'Glow Studio',
        businessEmail: 'hello@example.com',
        ownerEmail: 'owner@example.com',
        password: 'DemoPass123!',
      }),
    )
  })

  it('shows registration errors and redirects authenticated users', async () => {
    mockHttp('post').mockRejectedValueOnce(
      apiError('Email is already registered.', { status: 409 }),
    )
    const { user, unmount } = renderWithProviders(<RegisterTestRoutes />, {
      route: '/register',
    })

    await user.type(screen.getByLabelText(/^Business name/), 'Glow Studio')
    await user.type(
      screen.getByLabelText(/^Business email/),
      'hello@example.com',
    )
    await user.type(screen.getByLabelText(/^Owner email/), 'owner@example.com')
    await user.type(screen.getByLabelText(/^Password/), 'DemoPass123!')
    await user.click(screen.getByRole('button', { name: 'Create account' }))
    expect(
      await screen.findByText('Email is already registered.'),
    ).toBeInTheDocument()

    unmount()
    renderWithProviders(<RegisterTestRoutes />, {
      route: '/register',
      authSession: authResponse,
    })
    expect(await screen.findByText('Authenticated home')).toBeInTheDocument()
  })
})
