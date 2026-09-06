import { describe, expect, it } from 'vitest'

import { apiResponse, mockHttp } from '../../test/httpMock.js'
import {
  fireEvent,
  renderWithProviders,
  screen,
  waitFor,
} from '../../test/testUtils.jsx'
import AppointmentListPage from './AppointmentListPage.jsx'

const emptyPage = {
  content: [],
  page: 0,
  size: 20,
  totalElements: 0,
  totalPages: 0,
}

describe('AppointmentListPage', () => {
  it('sends status and local date filters to the server', async () => {
    const get = mockHttp('get').mockImplementation((url) => {
      if (url === '/api/staff') {
        return Promise.resolve(apiResponse({ ...emptyPage, size: 100 }))
      }
      if (url === '/api/appointments') {
        return Promise.resolve(apiResponse(emptyPage))
      }
      return Promise.reject(new Error(`Unexpected GET ${url}`))
    })
    const { user } = renderWithProviders(<AppointmentListPage />, {
      route: '/appointments',
      authSession: {
        accessToken: 'owner-token',
        user: { email: 'owner@example.com', role: 'OWNER' },
      },
    })

    await waitFor(() =>
      expect(
        get.mock.calls.some(([url]) => url === '/api/appointments'),
      ).toBe(true),
    )

    await user.click(screen.getByRole('combobox', { name: 'Status' }))
    await user.click(await screen.findByRole('option', { name: 'Cancelled' }))

    await waitFor(() =>
      expect(
        get.mock.calls.some(
          ([url, config]) =>
            url === '/api/appointments' &&
            config.params.status === 'CANCELLED',
        ),
      ).toBe(true),
    )

    const localFrom = '2026-09-12T09:30'
    fireEvent.change(screen.getByLabelText('From'), {
      target: { value: localFrom },
    })

    await waitFor(() =>
      expect(
        get.mock.calls.some(
          ([url, config]) =>
            url === '/api/appointments' &&
            config.params.status === 'CANCELLED' &&
            config.params.from === new Date(localFrom).toISOString(),
        ),
      ).toBe(true),
    )
  })
})
