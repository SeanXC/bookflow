import { Route, Routes } from 'react-router-dom'
import { describe, expect, it } from 'vitest'

import { apiResponse, mockHttp } from '../../test/httpMock.js'
import {
  renderWithProviders,
  screen,
  waitFor,
} from '../../test/testUtils.jsx'
import LoginPage from './LoginPage.jsx'

function LoginTestRoutes() {
  return (
    <Routes>
      <Route element={<LoginPage />} path="/login" />
      <Route element={<div>Authenticated home</div>} path="/" />
    </Routes>
  )
}

describe('LoginPage', () => {
  it('does not submit while required fields are empty', async () => {
    const post = mockHttp('post')
    const { user } = renderWithProviders(<LoginTestRoutes />, {
      route: '/login',
    })

    await user.click(screen.getByRole('button', { name: 'Sign in' }))

    expect(screen.getByLabelText(/^Email address/)).toBeInvalid()
    expect(screen.getByLabelText(/^Password/)).toBeInvalid()
    expect(post).not.toHaveBeenCalled()
  })

  it('stores a successful login and navigates home', async () => {
    const post = mockHttp('post').mockResolvedValueOnce(
      apiResponse({
        accessToken: 'owner-token',
        user: {
          email: 'owner@example.com',
          role: 'OWNER',
        },
      }),
    )
    const { user } = renderWithProviders(<LoginTestRoutes />, {
      route: '/login',
    })

    await user.type(
      screen.getByLabelText(/^Email address/),
      'OWNER@EXAMPLE.COM',
    )
    await user.type(screen.getByLabelText(/^Password/), 'correct-password')
    await user.click(screen.getByRole('button', { name: 'Sign in' }))

    await waitFor(() =>
      expect(post).toHaveBeenCalledWith('/api/auth/login', {
        email: 'owner@example.com',
        password: 'correct-password',
      }),
    )
    expect(
      await screen.findByText('Authenticated home'),
    ).toBeInTheDocument()
  })
})
