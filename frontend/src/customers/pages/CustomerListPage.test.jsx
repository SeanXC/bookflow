import { describe, expect, it } from 'vitest'

import { apiError, apiResponse, mockHttp } from '../../test/httpMock.js'
import {
  renderWithProviders,
  screen,
  waitFor,
  within,
} from '../../test/testUtils.jsx'
import CustomerListPage from './CustomerListPage.jsx'

const emptyPage = {
  content: [],
  page: 0,
  size: 20,
  totalElements: 0,
  totalPages: 0,
}

const session = {
  accessToken: 'reception-token',
  user: {
    email: 'reception@example.com',
    role: 'RECEPTIONIST',
  },
}

describe('CustomerListPage', () => {
  it('debounces customer search and sends it to the server', async () => {
    const get = mockHttp('get').mockResolvedValue(apiResponse(emptyPage))
    const { user } = renderWithProviders(<CustomerListPage />, {
      route: '/customers',
      authSession: session,
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

  it('renders contact details and opens create and edit dialogs', async () => {
    mockHttp('get').mockResolvedValueOnce(
      apiResponse({
        ...emptyPage,
        content: [
          {
            id: 5,
            firstName: 'Alice',
            lastName: 'Murphy',
            email: 'alice@example.com',
            phone: '0871234567',
            notes: 'Prefers afternoons',
            createdAt: '2026-09-01T09:00:00Z',
          },
          {
            id: 6,
            firstName: 'Ben',
            lastName: 'Walsh',
            email: null,
            phone: null,
            notes: null,
            createdAt: '2026-09-02T09:00:00Z',
          },
        ],
        totalElements: 2,
        totalPages: 1,
      }),
    )
    const { user } = renderWithProviders(<CustomerListPage />, {
      route: '/customers',
      authSession: session,
    })

    expect(await screen.findByText('Alice Murphy')).toBeInTheDocument()
    expect(screen.getByText('Ben Walsh')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'alice@example.com' })).toHaveAttribute(
      'href',
      'mailto:alice@example.com',
    )

    await user.click(screen.getByRole('button', { name: 'Add customer' }))
    expect(
      within(screen.getByRole('dialog')).getByText('Create customer'),
    ).toBeInTheDocument()
    await user.click(
      within(screen.getByRole('dialog')).getByRole('button', { name: 'Cancel' }),
    )

    await user.click(screen.getAllByRole('button', { name: 'Edit' })[0])
    expect(
      within(screen.getByRole('dialog')).getByText('Edit customer'),
    ).toBeInTheDocument()
    await user.click(
      within(screen.getByRole('dialog')).getByRole('button', { name: 'Cancel' }),
    )

    await user.click(screen.getAllByRole('button', { name: 'View' })[0])
  })

  it('shows list errors and retries customer loading', async () => {
    const get = mockHttp('get')
      .mockRejectedValueOnce(apiError('Unable to load customers.'))
      .mockResolvedValueOnce(apiResponse(emptyPage))
    const { user } = renderWithProviders(<CustomerListPage />, {
      route: '/customers',
      authSession: session,
    })

    expect(
      await screen.findByText('Unable to load customers.'),
    ).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Retry' }))

    await waitFor(() => expect(get).toHaveBeenCalledTimes(2))
  })
})
