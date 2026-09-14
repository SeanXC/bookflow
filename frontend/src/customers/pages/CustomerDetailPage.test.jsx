import { Route, Routes } from 'react-router-dom'
import { describe, expect, it } from 'vitest'

import { apiError, apiResponse, mockHttp } from '../../test/httpMock.js'
import {
  renderWithProviders,
  screen,
  waitFor,
  within,
} from '../../test/testUtils.jsx'
import CustomerDetailPage from './CustomerDetailPage.jsx'

const session = {
  accessToken: 'reception-token',
  user: { email: 'reception@example.com', role: 'RECEPTIONIST' },
}

const customer = {
  id: 5,
  firstName: 'Alice',
  lastName: 'Murphy',
  email: 'alice@example.com',
  phone: '0871234567',
  notes: 'Prefers afternoons',
  createdAt: '2026-09-01T09:00:00Z',
}

const appointmentsPage = {
  content: [
    {
      id: 91,
      startTime: '2026-09-14T10:30:00Z',
      status: 'COMPLETED',
      service: { name: 'Consultation' },
      staff: { firstName: 'Sam', lastName: 'Kelly' },
    },
  ],
  page: 0,
  size: 10,
  totalElements: 1,
  totalPages: 1,
}

function renderPage(route = '/customers/5') {
  return renderWithProviders(
    <Routes>
      <Route
        element={<CustomerDetailPage />}
        path="/customers/:customerId"
      />
    </Routes>,
    { route, authSession: session },
  )
}

describe('CustomerDetailPage', () => {
  it('renders customer details, appointment history, and the edit dialog', async () => {
    const get = mockHttp('get').mockImplementation((url) => {
      if (url === '/api/customers/5') {
        return Promise.resolve(apiResponse(customer))
      }
      if (url === '/api/customers/5/appointments') {
        return Promise.resolve(apiResponse(appointmentsPage))
      }
      return Promise.reject(new Error(`Unexpected GET ${url}`))
    })
    const { user } = renderPage()

    expect(
      await screen.findByRole('heading', { name: 'Alice Murphy' }),
    ).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'alice@example.com' })).toHaveAttribute(
      'href',
      'mailto:alice@example.com',
    )
    expect(screen.getByRole('link', { name: '0871234567' })).toHaveAttribute(
      'href',
      'tel:0871234567',
    )
    expect(screen.getByText('Prefers afternoons')).toBeInTheDocument()
    expect(await screen.findByText('Consultation')).toBeInTheDocument()
    expect(screen.getByText('Sam Kelly')).toBeInTheDocument()
    expect(screen.getByText('COMPLETED')).toBeInTheDocument()

    await waitFor(() =>
      expect(get).toHaveBeenCalledWith('/api/customers/5/appointments', {
        params: { page: 0, size: 10 },
      }),
    )

    await user.click(screen.getByRole('button', { name: 'Edit customer' }))
    expect(
      within(screen.getByRole('dialog')).getByRole('heading', {
        name: 'Edit customer',
      }),
    ).toBeInTheDocument()
    await user.click(
      within(screen.getByRole('dialog')).getByRole('button', { name: 'Cancel' }),
    )
  })

  it('rejects invalid customer route parameters without fetching', async () => {
    const get = mockHttp('get')

    renderPage('/customers/not-a-number')

    expect(await screen.findByText('Invalid customer ID.')).toBeInTheDocument()
    expect(get).not.toHaveBeenCalled()
  })

  it('shows customer errors and retries the detail request', async () => {
    let customerRequests = 0
    mockHttp('get').mockImplementation((url) => {
      if (url === '/api/customers/5') {
        customerRequests += 1
        return customerRequests === 1
          ? Promise.reject(apiError('Unable to load customer.'))
          : Promise.resolve(apiResponse(customer))
      }
      if (url === '/api/customers/5/appointments') {
        return Promise.resolve(
          apiResponse({ ...appointmentsPage, content: [], totalElements: 0 }),
        )
      }
      return Promise.reject(new Error(`Unexpected GET ${url}`))
    })
    const { user } = renderPage()

    expect(
      await screen.findByText('Unable to load customer.'),
    ).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Retry' }))

    expect(
      await screen.findByRole('heading', { name: 'Alice Murphy' }),
    ).toBeInTheDocument()
    expect(customerRequests).toBe(2)
  })

  it('shows appointment errors and retries the history request', async () => {
    let historyRequests = 0
    mockHttp('get').mockImplementation((url) => {
      if (url === '/api/customers/5') {
        return Promise.resolve(apiResponse(customer))
      }
      if (url === '/api/customers/5/appointments') {
        historyRequests += 1
        return historyRequests === 1
          ? Promise.reject(apiError('Unable to load appointment history.'))
          : Promise.resolve(apiResponse(appointmentsPage))
      }
      return Promise.reject(new Error(`Unexpected GET ${url}`))
    })
    const { user } = renderPage()

    expect(
      await screen.findByText('Unable to load appointment history.'),
    ).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Retry' }))

    expect(await screen.findByText('Consultation')).toBeInTheDocument()
    expect(historyRequests).toBe(2)
  })
})
