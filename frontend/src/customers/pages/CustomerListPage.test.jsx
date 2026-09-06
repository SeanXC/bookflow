import { describe, expect, it } from 'vitest'

import { apiResponse, mockHttp } from '../../test/httpMock.js'
import {
  renderWithProviders,
  screen,
  waitFor,
} from '../../test/testUtils.jsx'
import CustomerListPage from './CustomerListPage.jsx'

const emptyPage = {
  content: [],
  page: 0,
  size: 20,
  totalElements: 0,
  totalPages: 0,
}

describe('CustomerListPage', () => {
  it('debounces customer search and sends it to the server', async () => {
    const get = mockHttp('get').mockResolvedValue(apiResponse(emptyPage))
    const { user } = renderWithProviders(<CustomerListPage />, {
      route: '/customers',
      authSession: {
        accessToken: 'reception-token',
        user: {
          email: 'reception@example.com',
          role: 'RECEPTIONIST',
        },
      },
    })

    await waitFor(() => expect(get).toHaveBeenCalledTimes(1))
    await user.type(screen.getByLabelText('Search customers'), '  Alice  ')

    await waitFor(() =>
      expect(get).toHaveBeenLastCalledWith('/api/customers', {
        params: {
          page: 0,
          size: 20,
          sort: ['lastName,asc'],
          search: 'Alice',
        },
      }),
    )
  })
})
