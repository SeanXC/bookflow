import { describe, expect, it } from 'vitest'

import { apiError, apiResponse, mockHttp } from '../../test/httpMock.js'
import {
  renderWithProviders,
  screen,
  waitFor,
  within,
} from '../../test/testUtils.jsx'
import ServiceListPage from './ServiceListPage.jsx'

const service = {
  id: 22,
  name: 'Consultation',
  description: null,
  price: 45,
  durationMinutes: 60,
  active: true,
}

const servicePage = {
  content: [service],
  page: 0,
  size: 20,
  totalElements: 1,
  totalPages: 1,
}

const ownerSession = {
  accessToken: 'owner-token',
  user: { email: 'owner@example.com', role: 'OWNER' },
}

const receptionistSession = {
  accessToken: 'reception-token',
  user: { email: 'reception@example.com', role: 'RECEPTIONIST' },
}

describe('ServiceListPage', () => {
  it('lets owners inspect, edit, create, and deactivate services', async () => {
    mockHttp('get').mockResolvedValue(apiResponse(servicePage))
    const del = mockHttp('delete').mockResolvedValue(apiResponse(null))
    const { user } = renderWithProviders(<ServiceListPage />, {
      route: '/services',
      authSession: ownerSession,
    })

    expect(await screen.findByText('Consultation')).toBeInTheDocument()
    expect(screen.getByText('€45.00')).toBeInTheDocument()
    expect(screen.getByText('60 min')).toBeInTheDocument()
    expect(screen.getByText('—')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Add service' }))
    expect(screen.getByText('Create service')).toBeInTheDocument()
    await user.click(
      within(screen.getByRole('dialog')).getByRole('button', { name: 'Cancel' }),
    )

    await user.click(screen.getByRole('button', { name: 'Edit' }))
    expect(screen.getByText('Edit service')).toBeInTheDocument()
    await user.click(
      within(screen.getByRole('dialog')).getByRole('button', { name: 'Cancel' }),
    )

    await user.click(screen.getByRole('button', { name: 'Deactivate' }))
    const dialog = screen.getByRole('dialog')
    expect(
      within(dialog).getByText(
        'Consultation will no longer be available for new appointments.',
      ),
    ).toBeInTheDocument()
    await user.click(
      within(dialog).getByRole('button', { name: 'Deactivate' }),
    )

    await waitFor(() =>
      expect(del).toHaveBeenCalledWith('/api/services/22'),
    )
  })

  it('keeps service management actions hidden from receptionists', async () => {
    mockHttp('get').mockResolvedValue(apiResponse(servicePage))

    renderWithProviders(<ServiceListPage />, {
      route: '/services',
      authSession: receptionistSession,
    })

    expect(await screen.findByText('Consultation')).toBeInTheDocument()
    expect(
      screen.queryByRole('button', { name: 'Add service' }),
    ).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Edit' })).not.toBeInTheDocument()
    expect(
      screen.queryByRole('button', { name: 'Deactivate' }),
    ).not.toBeInTheDocument()
  })

  it('sends status and debounced search filters to the API', async () => {
    const get = mockHttp('get').mockResolvedValue(apiResponse(servicePage))
    const { user } = renderWithProviders(<ServiceListPage />, {
      route: '/services',
      authSession: ownerSession,
    })

    await waitFor(() => expect(get).toHaveBeenCalledTimes(1))
    await user.click(screen.getByRole('combobox', { name: 'Status' }))
    await user.click(await screen.findByRole('option', { name: 'Inactive' }))

    await waitFor(() =>
      expect(
        get.mock.calls.some(
          ([url, config]) =>
            url === '/api/services' && config.params.active === false,
        ),
      ).toBe(true),
    )

    await user.type(screen.getByLabelText('Search services'), '  Hair  ')

    await waitFor(() =>
      expect(
        get.mock.calls.some(
          ([url, config]) =>
            url === '/api/services' &&
            config.params.active === false &&
            config.params.search === 'Hair',
        ),
      ).toBe(true),
    )
  })

  it('shows list errors and retries the failed request', async () => {
    const get = mockHttp('get')
      .mockRejectedValueOnce(apiError('Unable to load services.'))
      .mockResolvedValue(apiResponse(servicePage))
    const { user } = renderWithProviders(<ServiceListPage />, {
      route: '/services',
      authSession: ownerSession,
    })

    expect(
      await screen.findByText('Unable to load services.'),
    ).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Retry' }))

    expect(await screen.findByText('Consultation')).toBeInTheDocument()
    expect(get).toHaveBeenCalledTimes(2)
  })
})
