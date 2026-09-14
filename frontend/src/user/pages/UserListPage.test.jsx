import { describe, expect, it } from 'vitest'

import { apiError, apiResponse, mockHttp } from '../../test/httpMock.js'
import {
  renderWithProviders,
  screen,
  waitFor,
} from '../../test/testUtils.jsx'
import UserListPage from './UserListPage.jsx'

const ownerSession = {
  accessToken: 'owner-token',
  user: { email: 'owner@example.com', role: 'OWNER' },
}

describe('UserListPage', () => {
  it('loads the first account page with stable email sorting', async () => {
    const get = mockHttp('get').mockResolvedValueOnce(
      apiResponse({
        content: [],
        page: 0,
        size: 20,
        totalElements: 0,
        totalPages: 0,
      }),
    )
    renderWithProviders(<UserListPage />, {
      route: '/users',
      authSession: ownerSession,
    })

    expect(
      screen.getByRole('heading', { name: 'Users' }),
    ).toBeInTheDocument()
    await waitFor(() =>
      expect(get).toHaveBeenCalledWith('/api/users', {
        params: {
          page: 0,
          size: 20,
          sort: ['email,asc'],
        },
      }),
    )
  })

  it('opens account creation and enablement dialogs for managed users', async () => {
    mockHttp('get').mockResolvedValueOnce(
      apiResponse({
        content: [
          {
            id: 1,
            email: 'owner@example.com',
            role: 'OWNER',
            enabled: true,
            createdAt: '2026-09-01T09:00:00Z',
          },
          {
            id: 2,
            email: 'staff@example.com',
            role: 'STAFF',
            enabled: true,
            createdAt: '2026-09-02T09:00:00Z',
          },
          {
            id: 3,
            email: 'reception@example.com',
            role: 'RECEPTIONIST',
            enabled: false,
            createdAt: '2026-09-03T09:00:00Z',
          },
        ],
        page: 0,
        size: 20,
        totalElements: 3,
        totalPages: 1,
      }),
    )
    const { user } = renderWithProviders(<UserListPage />, {
      route: '/users',
      authSession: ownerSession,
    })

    expect(await screen.findByText('staff@example.com')).toBeInTheDocument()
    expect(screen.getAllByText('Enabled')).toHaveLength(2)
    expect(screen.getByText('Disabled')).toBeInTheDocument()
    expect(screen.getAllByRole('button', { name: 'Disable' })).toHaveLength(1)
    expect(screen.getAllByRole('button', { name: 'Enable' })).toHaveLength(1)

    await user.click(screen.getByRole('button', { name: 'Create account' }))
    expect(
      screen.getByRole('heading', { name: 'Create login account' }),
    ).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Cancel' }))

    await user.click(screen.getByRole('button', { name: 'Disable' }))
    expect(
      screen.getByRole('heading', { name: 'Disable login account' }),
    ).toBeInTheDocument()
    expect(
      screen.getByText('Prevent this user from signing in and using the API?'),
    ).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Cancel' }))

    await user.click(screen.getByRole('button', { name: 'Enable' }))
    expect(
      screen.getByRole('heading', { name: 'Enable login account' }),
    ).toBeInTheDocument()
    expect(
      screen.getByText('Allow this user to sign in again?'),
    ).toBeInTheDocument()
  })

  it('shows list errors and retries loading users', async () => {
    const get = mockHttp('get')
      .mockRejectedValueOnce(apiError('Unable to load users.'))
      .mockResolvedValueOnce(
        apiResponse({
          content: [],
          page: 0,
          size: 20,
          totalElements: 0,
          totalPages: 0,
        }),
      )
    const { user } = renderWithProviders(<UserListPage />, {
      route: '/users',
      authSession: ownerSession,
    })

    expect(await screen.findByText('Unable to load users.')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Retry' }))

    await waitFor(() => expect(get).toHaveBeenCalledTimes(2))
    expect(
      await screen.findByText('No rows'),
    ).toBeInTheDocument()
  })
})
