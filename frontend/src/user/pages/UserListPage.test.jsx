import { describe, expect, it } from 'vitest'

import { apiResponse, mockHttp } from '../../test/httpMock.js'
import {
  renderWithProviders,
  screen,
  waitFor,
} from '../../test/testUtils.jsx'
import UserListPage from './UserListPage.jsx'

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
      authSession: {
        accessToken: 'owner-token',
        user: { email: 'owner@example.com', role: 'OWNER' },
      },
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
})
