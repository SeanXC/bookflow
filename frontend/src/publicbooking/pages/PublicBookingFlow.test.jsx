import { Route, Routes } from 'react-router-dom'
import { describe, expect, it } from 'vitest'

import { apiError, apiResponse, mockHttp } from '../../test/httpMock.js'
import {
  renderWithProviders,
  screen,
  waitFor,
} from '../../test/testUtils.jsx'
import { defaultSlotRange, formatSlotTime } from '../format.js'
import PublicBookingPage from './PublicBookingPage.jsx'
import PublicBusinessPage from './PublicBusinessPage.jsx'
import PublicConfirmationPage from './PublicConfirmationPage.jsx'
import PublicServicePage from './PublicServicePage.jsx'

const SLOT_START = '2026-09-14T09:00:00Z'
const SLOT_END = '2026-09-14T10:00:00Z'

const business = {
  slug: 'glow-studio',
  name: 'Glow Studio',
  phone: '0871112222',
  description: 'Colour and cuts',
}

const haircut = {
  id: 50,
  name: 'Haircut',
  description: 'Classic cut',
  price: 30,
  durationMinutes: 60,
}

const anna = {
  id: 40,
  firstName: 'Anna',
  lastName: 'Smith',
}

const appointment = {
  id: 99,
  staff: anna,
  service: haircut,
  customerFirstName: 'Emma',
  customerLastName: 'Smith',
  startTime: SLOT_START,
  endTime: SLOT_END,
  status: 'CONFIRMED',
  notes: 'Please use the side door',
}

function PublicBookingRoutes() {
  return (
    <Routes>
      <Route element={<PublicBusinessPage />} path="/book/:slug" />
      <Route
        element={<PublicServicePage />}
        path="/book/:slug/services/:serviceId"
      />
      <Route
        element={<PublicBookingPage />}
        path="/book/:slug/services/:serviceId/staff/:staffId"
      />
      <Route
        element={<PublicConfirmationPage />}
        path="/book/:slug/confirmation"
      />
    </Routes>
  )
}

function stubPublicCatalog() {
  return mockHttp('get').mockImplementation((url) => {
    if (url === '/api/public/businesses/glow-studio') {
      return Promise.resolve(apiResponse(business))
    }
    if (url === '/api/public/businesses/glow-studio/services') {
      return Promise.resolve(apiResponse([haircut]))
    }
    if (url === '/api/public/businesses/glow-studio/staff') {
      return Promise.resolve(apiResponse([anna]))
    }
    if (url === '/api/public/businesses/glow-studio/staff/40/slots') {
      return Promise.resolve(
        apiResponse([{ startTime: SLOT_START, endTime: SLOT_END }]),
      )
    }
    return Promise.reject(new Error(`Unexpected GET ${url}`))
  })
}

describe('public booking flow', () => {
  it('hides a disabled or unknown booking page', async () => {
    mockHttp('get').mockImplementation((url) => {
      if (
        url === '/api/public/businesses/glow-studio' ||
        url === '/api/public/businesses/glow-studio/services'
      ) {
        return Promise.reject(
          apiError('Business not found: glow-studio', {
            status: 404,
            code: 'RESOURCE_NOT_FOUND',
          }),
        )
      }
      return Promise.reject(new Error(`Unexpected GET ${url}`))
    })

    renderWithProviders(<PublicBookingRoutes />, {
      route: '/book/glow-studio',
    })

    expect(
      await screen.findByRole('heading', {
        name: 'Booking page unavailable',
      }),
    ).toBeInTheDocument()
    expect(
      screen.getByText(
        'This business is not accepting online bookings right now.',
      ),
    ).toBeInTheDocument()
  })

  it('books a selected slot and shows the confirmation page', async () => {
    const range = defaultSlotRange()
    const get = stubPublicCatalog()
    const post = mockHttp('post').mockResolvedValueOnce(
      apiResponse(appointment),
    )
    const { user } = renderWithProviders(<PublicBookingRoutes />, {
      route: '/book/glow-studio',
    })

    expect(
      await screen.findByRole('heading', { name: 'Glow Studio' }),
    ).toBeInTheDocument()
    expect(screen.getByText('Colour and cuts')).toBeInTheDocument()
    expect(
      screen.getByRole('link', { name: /Book with the assistant/ }),
    ).toHaveAttribute('href', '/book/glow-studio/assistant')

    await user.click(screen.getByRole('link', { name: /Haircut/ }))
    expect(
      await screen.findByRole('heading', { name: 'Haircut' }),
    ).toBeInTheDocument()

    await user.click(screen.getByRole('link', { name: /Anna Smith/ }))
    expect(
      await screen.findByRole('heading', { name: 'Book Haircut' }),
    ).toBeInTheDocument()

    await waitFor(() =>
      expect(get).toHaveBeenCalledWith(
        '/api/public/businesses/glow-studio/staff/40/slots',
        {
          params: {
            serviceId: 50,
            from: range.from,
            to: range.to,
          },
        },
      ),
    )

    await user.click(
      screen.getByRole('button', { name: formatSlotTime(SLOT_START) }),
    )
    await user.type(screen.getByLabelText(/^First name/), 'Emma')
    await user.type(screen.getByLabelText(/^Last name/), 'Smith')
    await user.type(screen.getByLabelText(/^Email/), 'EMMA@EXAMPLE.COM')
    await user.type(screen.getByLabelText(/^Phone/), '0871112222')
    await user.type(
      screen.getByLabelText('Notes (optional)'),
      'Please use the side door',
    )
    await user.click(screen.getByRole('button', { name: 'Confirm booking' }))

    await waitFor(() =>
      expect(post).toHaveBeenCalledWith(
        '/api/public/businesses/glow-studio/appointments',
        {
          staffId: 40,
          serviceId: 50,
          startTime: SLOT_START,
          firstName: 'Emma',
          lastName: 'Smith',
          email: 'emma@example.com',
          phone: '0871112222',
          notes: 'Please use the side door',
        },
      ),
    )
    expect(
      await screen.findByRole('heading', { name: 'Appointment confirmed' }),
    ).toBeInTheDocument()
    expect(
      await screen.findByText("You're booked at Glow Studio."),
    ).toBeInTheDocument()
    expect(screen.getByText('Anna Smith')).toBeInTheDocument()
    expect(screen.getByText('Emma Smith')).toBeInTheDocument()
    expect(screen.getByText('Please use the side door')).toBeInTheDocument()
  })

  it('keeps the guest on the form after a booking conflict', async () => {
    stubPublicCatalog()
    mockHttp('post').mockRejectedValueOnce(
      apiError('This staff member already has an appointment.', {
        status: 409,
        code: 'BOOKING_CONFLICT',
      }),
    )
    const { user } = renderWithProviders(<PublicBookingRoutes />, {
      route: '/book/glow-studio/services/50/staff/40',
    })

    expect(
      await screen.findByRole('heading', { name: 'Book Haircut' }),
    ).toBeInTheDocument()
    await user.click(
      screen.getByRole('button', { name: formatSlotTime(SLOT_START) }),
    )
    await user.type(screen.getByLabelText(/^First name/), 'Emma')
    await user.type(screen.getByLabelText(/^Last name/), 'Smith')
    await user.type(screen.getByLabelText(/^Email/), 'emma@example.com')
    await user.type(screen.getByLabelText(/^Phone/), '0871112222')
    await user.click(screen.getByRole('button', { name: 'Confirm booking' }))

    expect(
      await screen.findByText(
        'This time is no longer available. Please choose another slot.',
      ),
    ).toBeInTheDocument()
    expect(
      screen.getByRole('heading', { name: 'Book Haircut' }),
    ).toBeInTheDocument()
  })

  it('does not show confirmation details without a completed booking', async () => {
    renderWithProviders(<PublicBookingRoutes />, {
      route: '/book/glow-studio/confirmation',
    })

    expect(
      await screen.findByRole('heading', {
        name: 'Confirmation unavailable',
      }),
    ).toBeInTheDocument()
    expect(
      screen.getByRole('link', { name: 'Back to booking page' }),
    ).toHaveAttribute('href', '/book/glow-studio')
  })
})
