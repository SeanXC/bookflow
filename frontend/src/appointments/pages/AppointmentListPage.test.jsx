import { describe, expect, it } from 'vitest'

import { apiError, apiResponse, mockHttp } from '../../test/httpMock.js'
import {
  fireEvent,
  renderWithProviders,
  screen,
  waitFor,
  within,
} from '../../test/testUtils.jsx'
import AppointmentListPage from './AppointmentListPage.jsx'

const emptyPage = {
  content: [],
  page: 0,
  size: 20,
  totalElements: 0,
  totalPages: 0,
}

const ownerSession = {
  accessToken: 'owner-token',
  user: { email: 'owner@example.com', role: 'OWNER' },
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
      authSession: ownerSession,
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

  it('lets owners filter and manage confirmed appointments', async () => {
    const appointment = {
      id: 81,
      startTime: '2026-09-14T09:00:00Z',
      endTime: '2026-09-14T10:00:00Z',
      status: 'CONFIRMED',
      customer: { id: 1, firstName: 'Alice', lastName: 'Murphy' },
      service: { id: 2, name: 'Consultation', price: 75 },
      staff: { id: 40, firstName: 'Anna', lastName: 'Smith' },
      notes: null,
    }
    const staffPage = {
      ...emptyPage,
      content: [appointment.staff],
      size: 100,
      totalElements: 1,
      totalPages: 1,
    }
    const get = mockHttp('get').mockImplementation((url) => {
      if (url === '/api/staff') {
        return Promise.resolve(apiResponse(staffPage))
      }
      if (url === '/api/appointments') {
        return Promise.resolve(
          apiResponse({
            ...emptyPage,
            content: [appointment],
            totalElements: 1,
            totalPages: 1,
          }),
        )
      }
      if (url === '/api/services' || url === '/api/customers') {
        return Promise.resolve(apiResponse(emptyPage))
      }
      return Promise.reject(new Error(`Unexpected GET ${url}`))
    })
    const { user } = renderWithProviders(<AppointmentListPage />, {
      route: '/appointments',
      authSession: ownerSession,
    })

    expect(await screen.findByText('Alice Murphy')).toBeInTheDocument()
    expect(screen.getByText('Consultation')).toBeInTheDocument()
    expect(screen.getByText('Anna Smith')).toBeInTheDocument()
    expect(screen.getByText('CONFIRMED')).toBeInTheDocument()

    await user.click(screen.getByRole('combobox', { name: 'Staff' }))
    await user.click(await screen.findByRole('option', { name: 'Anna Smith' }))
    await waitFor(() =>
      expect(
        get.mock.calls.some(
          ([url, config]) =>
            url === '/api/appointments' && config.params.staffId === 40,
        ),
      ).toBe(true),
    )

    fireEvent.change(screen.getByLabelText('To'), {
      target: { value: '2026-09-15T17:00' },
    })
    await user.click(screen.getByRole('button', { name: 'Clear' }))

    await user.click(screen.getByRole('button', { name: 'Create appointment' }))
    await user.click(
      within(screen.getByRole('dialog')).getByRole('button', { name: 'Cancel' }),
    )

    await user.click(screen.getByRole('button', { name: 'Edit' }))
    await user.click(
      within(screen.getByRole('dialog')).getByRole('button', { name: 'Cancel' }),
    )

    await user.click(screen.getByRole('button', { name: 'Complete' }))
    expect(
      screen.getByRole('heading', { name: 'Complete appointment' }),
    ).toBeInTheDocument()
    await user.click(
      within(screen.getByRole('dialog')).getByRole('button', {
        name: 'Go back',
      }),
    )

    await user.click(screen.getByRole('button', { name: 'Cancel' }))
    expect(
      screen.getByRole('heading', { name: 'Cancel appointment' }),
    ).toBeInTheDocument()
  }, 30000)

  it('warns about staff failures and retries appointment errors', async () => {
    let appointmentRequests = 0
    mockHttp('get').mockImplementation((url) => {
      if (url === '/api/staff') {
        return Promise.reject(apiError('Unable to load staff.'))
      }
      if (url === '/api/appointments') {
        appointmentRequests += 1
        return appointmentRequests === 1
          ? Promise.reject(apiError('Unable to load appointments.'))
          : Promise.resolve(apiResponse(emptyPage))
      }
      return Promise.reject(new Error(`Unexpected GET ${url}`))
    })
    const { user } = renderWithProviders(<AppointmentListPage />, {
      route: '/appointments',
      authSession: ownerSession,
    })

    expect(
      await screen.findByText(
        'Staff filters could not be loaded. Appointments are still available.',
      ),
    ).toBeInTheDocument()
    expect(
      await screen.findByText('Unable to load appointments.'),
    ).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Retry' }))

    await waitFor(() => expect(appointmentRequests).toBe(2))
  })
})
