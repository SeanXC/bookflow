import { describe, expect, it } from 'vitest'

import { apiError, apiResponse, mockHttp } from '../../test/httpMock.js'
import {
  renderWithProviders,
  screen,
  waitFor,
} from '../../test/testUtils.jsx'
import DashboardPage from './DashboardPage.jsx'

const ownerSession = {
  accessToken: 'owner-token',
  user: { email: 'owner@example.com', role: 'OWNER' },
}

const summary = {
  todayAppointments: 12,
  monthlyRevenue: 4500.5,
  activeCustomers: 320,
  cancellationRate: 4.5,
  businessTimeZone: 'Europe/Dublin',
}

const appointmentPage = {
  content: [
    {
      id: 901,
      startTime: '2026-09-14T10:30:00Z',
      status: 'CONFIRMED',
      customer: { firstName: 'Alice', lastName: 'Murphy' },
      service: { name: 'Consultation' },
      staff: { firstName: 'Sam', lastName: 'Kelly' },
    },
  ],
  page: 0,
  size: 5,
  totalElements: 1,
  totalPages: 1,
}

function successfulResponse(url) {
  if (url === '/api/dashboard/summary') {
    return apiResponse(summary)
  }
  if (url === '/api/dashboard/bookings-by-week') {
    return apiResponse([
      { date: '2026-09-14', bookings: 2 },
      { date: '2026-09-15', bookings: 5 },
    ])
  }
  if (url === '/api/dashboard/revenue-by-month') {
    return apiResponse([
      { month: '2026-08', revenue: 3200 },
      { month: '2026-09', revenue: 4100 },
    ])
  }
  if (url === '/api/appointments') {
    return apiResponse(appointmentPage)
  }
  throw new Error(`Unexpected GET ${url}`)
}

describe('DashboardPage', () => {
  it('renders business metrics, charts, and recent appointments', async () => {
    const get = mockHttp('get').mockImplementation((url) =>
      Promise.resolve(successfulResponse(url)),
    )

    renderWithProviders(<DashboardPage />, {
      route: '/dashboard',
      authSession: ownerSession,
    })

    expect(
      await screen.findByText('Business time zone: Europe/Dublin'),
    ).toBeInTheDocument()
    expect(screen.getByText('12')).toBeInTheDocument()
    expect(screen.getByText('€4,500.50')).toBeInTheDocument()
    expect(screen.getByText('320')).toBeInTheDocument()
    expect(screen.getByText('4.5%')).toBeInTheDocument()
    expect(
      screen.getByRole('img', {
        name: 'Bar chart of appointments for each day this week',
      }),
    ).toBeInTheDocument()
    expect(
      screen.getByRole('img', {
        name: 'Line chart of revenue for the trailing twelve months',
      }),
    ).toBeInTheDocument()
    expect(screen.getByText('Alice Murphy')).toBeInTheDocument()
    expect(screen.getByText('Consultation')).toBeInTheDocument()
    expect(screen.getByText('Sam Kelly')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: /View all/ })).toHaveAttribute(
      'href',
      '/appointments',
    )

    await waitFor(() =>
      expect(get).toHaveBeenCalledWith('/api/appointments', {
        params: {
          page: 0,
          size: 5,
          sort: ['startTime,desc', 'id,desc'],
        },
      }),
    )
  })

  it('shows query errors and retries the summary request', async () => {
    let summaryRequests = 0
    mockHttp('get').mockImplementation((url) => {
      if (url === '/api/dashboard/summary') {
        summaryRequests += 1
        return summaryRequests === 1
          ? Promise.reject(apiError('Unable to load summary.'))
          : Promise.resolve(apiResponse(summary))
      }
      if (url === '/api/dashboard/bookings-by-week') {
        return Promise.reject(apiError('Unable to load bookings.'))
      }
      if (url === '/api/dashboard/revenue-by-month') {
        return Promise.reject(apiError('Unable to load revenue.'))
      }
      if (url === '/api/appointments') {
        return Promise.reject(apiError('Unable to load appointments.'))
      }
      return Promise.reject(new Error(`Unexpected GET ${url}`))
    })
    const { user } = renderWithProviders(<DashboardPage />, {
      route: '/dashboard',
      authSession: ownerSession,
    })

    expect(await screen.findByText('Unable to load summary.')).toBeInTheDocument()
    expect(screen.getByText('Unable to load bookings.')).toBeInTheDocument()
    expect(screen.getByText('Unable to load revenue.')).toBeInTheDocument()
    expect(screen.getByText('Unable to load appointments.')).toBeInTheDocument()

    await user.click(screen.getByRole('button', { name: 'Retry' }))

    expect(
      await screen.findByText('Business time zone: Europe/Dublin'),
    ).toBeInTheDocument()
    expect(summaryRequests).toBe(2)
  })

  it('renders an explicit empty state for recent appointments', async () => {
    mockHttp('get').mockImplementation((url) => {
      const response = successfulResponse(url)
      return Promise.resolve(
        url === '/api/appointments'
          ? apiResponse({ ...appointmentPage, content: [], totalElements: 0 })
          : response,
      )
    })

    renderWithProviders(<DashboardPage />, {
      route: '/dashboard',
      authSession: ownerSession,
    })

    expect(await screen.findByText('No appointments yet.')).toBeInTheDocument()
  })
})
