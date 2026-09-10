import { Route, Routes } from 'react-router-dom'
import { describe, expect, it } from 'vitest'

import { apiError, apiResponse, mockHttp } from '../../test/httpMock.js'
import {
  renderWithProviders,
  screen,
  waitFor,
} from '../../test/testUtils.jsx'
import { ASSISTANT_CHAT_TIMEOUT_MS } from '../constants.js'
import PublicAssistantPage from './PublicAssistantPage.jsx'
import PublicConfirmationPage from './PublicConfirmationPage.jsx'

const SLOT_START = '2026-09-14T09:00:00Z'
const SLOT_END = '2026-09-14T10:00:00Z'

const business = {
  slug: 'glow-studio',
  name: 'Glow Studio',
  phone: '0871112222',
  description: 'Colour and cuts',
}

const proposal = {
  proposalId: 'prop-1',
  requiresConfirmation: true,
  staffId: 40,
  staffFirstName: 'Anna',
  staffLastName: 'Smith',
  serviceId: 50,
  serviceName: 'Haircut',
  price: 30,
  durationMinutes: 60,
  startTime: SLOT_START,
  endTime: SLOT_END,
  firstName: 'Emma',
  lastName: 'Smith',
  email: 'emma@example.com',
  phone: '0871112222',
  notes: null,
}

const booking = {
  appointmentId: 99,
  staffId: 40,
  staffFirstName: 'Anna',
  staffLastName: 'Smith',
  serviceId: 50,
  serviceName: 'Haircut',
  startTime: SLOT_START,
  endTime: SLOT_END,
  status: 'CONFIRMED',
  firstName: 'Emma',
  lastName: 'Smith',
}

function AssistantRoutes() {
  return (
    <Routes>
      <Route element={<PublicAssistantPage />} path="/book/:slug/assistant" />
      <Route
        element={<PublicConfirmationPage />}
        path="/book/:slug/confirmation"
      />
    </Routes>
  )
}

function stubBusiness() {
  return mockHttp('get').mockImplementation((url) => {
    if (url === '/api/public/businesses/glow-studio') {
      return Promise.resolve(apiResponse(business))
    }
    return Promise.reject(new Error(`Unexpected GET ${url}`))
  })
}

describe('public assistant page', () => {
  it('hides the assistant when the booking page is unavailable', async () => {
    mockHttp('get').mockRejectedValueOnce(
      apiError('Business not found: glow-studio', {
        status: 404,
        code: 'RESOURCE_NOT_FOUND',
      }),
    )

    renderWithProviders(<AssistantRoutes />, {
      route: '/book/glow-studio/assistant',
    })

    expect(
      await screen.findByRole('heading', {
        name: 'Booking page unavailable',
      }),
    ).toBeInTheDocument()
  })

  it('proposes a real slot and only books after confirm', async () => {
    stubBusiness()
    const post = mockHttp('post').mockImplementation((url) => {
      if (url === '/api/public/businesses/glow-studio/assistant') {
        return Promise.resolve(
          apiResponse({
            message: 'Please confirm your Haircut with Anna at 09:00.',
            proposal,
          }),
        )
      }
      if (url === '/api/public/businesses/glow-studio/assistant/confirm') {
        return Promise.resolve(apiResponse(booking))
      }
      return Promise.reject(new Error(`Unexpected POST ${url}`))
    })
    const { user } = renderWithProviders(<AssistantRoutes />, {
      route: '/book/glow-studio/assistant',
    })

    expect(
      await screen.findByRole('heading', { name: 'Book with the assistant' }),
    ).toBeInTheDocument()

    await user.type(
      screen.getByLabelText('Message'),
      'Haircut with Anna on Monday morning',
    )
    await user.click(screen.getByRole('button', { name: 'Send' }))

    expect(
      await screen.findByText('Please confirm your Haircut with Anna at 09:00.'),
    ).toBeInTheDocument()
    expect(screen.getByText('Review this booking')).toBeInTheDocument()
    expect(
      screen.getByText('Nothing is booked until you confirm.'),
    ).toBeInTheDocument()

    await waitFor(() =>
      expect(post).toHaveBeenCalledWith(
        '/api/public/businesses/glow-studio/assistant',
        {
          messages: [
            {
              role: 'USER',
              content: 'Haircut with Anna on Monday morning',
            },
          ],
        },
        { timeout: ASSISTANT_CHAT_TIMEOUT_MS },
      ),
    )
    expect(post).not.toHaveBeenCalledWith(
      '/api/public/businesses/glow-studio/assistant/confirm',
      expect.anything(),
    )

    await user.click(screen.getByRole('button', { name: 'Confirm booking' }))

    await waitFor(() =>
      expect(post).toHaveBeenCalledWith(
        '/api/public/businesses/glow-studio/assistant/confirm',
        { proposalId: 'prop-1' },
      ),
    )
    expect(
      await screen.findByRole('heading', { name: 'Appointment confirmed' }),
    ).toBeInTheDocument()
    expect(screen.getByText('Anna Smith')).toBeInTheDocument()
    expect(screen.getByText('Emma Smith')).toBeInTheDocument()
  })

  it('shows a guest-facing error when the assistant is rate limited', async () => {
    stubBusiness()
    mockHttp('post').mockRejectedValueOnce(
      apiError('Too many assistant requests. Please try again later.', {
        status: 429,
        code: 'RATE_LIMITED',
      }),
    )
    const { user } = renderWithProviders(<AssistantRoutes />, {
      route: '/book/glow-studio/assistant',
    })

    await screen.findByRole('heading', { name: 'Book with the assistant' })
    await user.type(screen.getByLabelText('Message'), 'Hi')
    await user.click(screen.getByRole('button', { name: 'Send' }))

    expect(
      await screen.findByText(
        'Too many assistant requests. Please try again later.',
      ),
    ).toBeInTheDocument()
    expect(screen.queryByText('Review this booking')).not.toBeInTheDocument()
  })
})
