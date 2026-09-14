import { describe, expect, it } from 'vitest'

import { apiError, apiResponse, mockHttp } from '../../test/httpMock.js'
import {
  renderWithProviders,
  screen,
  waitFor,
  within,
} from '../../test/testUtils.jsx'
import StaffListPage from './StaffListPage.jsx'

const ownerSession = {
  accessToken: 'owner-token',
  user: { email: 'owner@example.com', role: 'OWNER' },
}

const emptyPage = {
  content: [],
  page: 0,
  size: 20,
  totalElements: 0,
  totalPages: 0,
}

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

  it('lets owners filter staff and open management dialogs', async () => {
    const staffPage = {
      ...emptyPage,
      content: [
        {
          id: 40,
          firstName: 'Anna',
          lastName: 'Smith',
          phone: '0871234567',
          userId: 7,
          active: true,
        },
        {
          id: 41,
          firstName: 'Ben',
          lastName: 'Jones',
          phone: null,
          userId: null,
          active: false,
        },
      ],
      totalElements: 2,
      totalPages: 1,
    }
    const get = mockHttp('get').mockImplementation((url) => {
      if (url === '/api/users') {
        return Promise.resolve(
          apiResponse({
            ...emptyPage,
            content: [
              {
                id: 7,
                email: 'anna@example.com',
                role: 'STAFF',
                enabled: true,
              },
            ],
          }),
        )
      }
      if (url === '/api/staff') {
        return Promise.resolve(apiResponse(staffPage))
      }
      return Promise.reject(new Error(`Unexpected GET ${url}`))
    })
    const { user } = renderWithProviders(<StaffListPage />, {
      route: '/staff',
      authSession: ownerSession,
    })

    expect(await screen.findByText('Anna Smith')).toBeInTheDocument()
    expect(screen.getByText('Ben Jones')).toBeInTheDocument()
    expect(screen.getByText('Linked')).toBeInTheDocument()
    expect(screen.getByText('Not linked')).toBeInTheDocument()

    await user.type(screen.getByLabelText('Search staff'), ' Anna ')
    await waitFor(
      () =>
        expect(
          get.mock.calls.some(
            ([url, config]) =>
              url === '/api/staff' && config.params.search === 'Anna',
          ),
        ).toBe(true),
      { timeout: 2000 },
    )

    await user.click(screen.getByRole('combobox', { name: 'Status' }))
    await user.click(await screen.findByRole('option', { name: 'All staff' }))
    await waitFor(() =>
      expect(
        get.mock.calls.some(
          ([url, config]) =>
            url === '/api/staff' && !('active' in config.params),
        ),
      ).toBe(true),
    )

    await user.click(screen.getByRole('button', { name: 'Add staff' }))
    await user.click(
      within(screen.getByRole('dialog')).getByRole('button', { name: 'Cancel' }),
    )

    await user.click(screen.getAllByRole('button', { name: 'Edit' })[0])
    expect(
      within(screen.getByRole('dialog')).getByText('Edit staff'),
    ).toBeInTheDocument()
    await user.click(
      within(screen.getByRole('dialog')).getByRole('button', { name: 'Cancel' }),
    )

    await user.click(screen.getByRole('button', { name: 'Deactivate' }))
    expect(
      within(screen.getByRole('dialog')).getByText('Deactivate staff member?'),
    ).toBeInTheDocument()
  })

  it('warns about account failures and retries staff errors', async () => {
    let staffRequests = 0
    mockHttp('get').mockImplementation((url) => {
      if (url === '/api/users') {
        return Promise.reject(apiError('Unable to load users.'))
      }
      if (url === '/api/staff') {
        staffRequests += 1
        return staffRequests === 1
          ? Promise.reject(apiError('Unable to load staff.'))
          : Promise.resolve(apiResponse(emptyPage))
      }
      return Promise.reject(new Error(`Unexpected GET ${url}`))
    })
    const { user } = renderWithProviders(<StaffListPage />, {
      route: '/staff',
      authSession: ownerSession,
    })

    expect(
      await screen.findByText(
        /Login accounts could not be loaded/,
      ),
    ).toBeInTheDocument()
    expect(await screen.findByText('Unable to load staff.')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Retry' }))

    await waitFor(() => expect(staffRequests).toBe(2))
  })
})
