import { describe, expect, it } from 'vitest'

import { apiResponse, mockHttp } from '../../test/httpMock.js'
import { renderWithProviders, screen } from '../../test/testUtils.jsx'
import StaffListPage from './StaffListPage.jsx'

describe('StaffListPage', () => {
  it('lets receptionists open staff hours', async () => {
    mockHttp('get').mockResolvedValue(
      apiResponse({
        content: [
          {
            id: 40,
            firstName: 'Anna',
            lastName: 'Smith',
            phone: null,
            userId: null,
            active: true,
          },
        ],
        page: 0,
        size: 20,
        totalElements: 1,
        totalPages: 1,
      }),
    )
    renderWithProviders(<StaffListPage />, {
      route: '/staff',
      authSession: {
        accessToken: 'reception-token',
        user: {
          email: 'reception@example.com',
          role: 'RECEPTIONIST',
        },
      },
    })

    expect(
      await screen.findByRole('button', { name: 'Hours' }),
    ).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Edit' })).not.toBeInTheDocument()
  })
})
