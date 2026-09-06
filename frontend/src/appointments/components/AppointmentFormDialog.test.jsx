import { describe, expect, it, vi } from 'vitest'

import { apiError, apiResponse, mockHttp } from '../../test/httpMock.js'
import {
  fireEvent,
  renderWithProviders,
  screen,
  waitFor,
} from '../../test/testUtils.jsx'
import AppointmentFormDialog from './AppointmentFormDialog.jsx'

function pageWith(item) {
  return {
    content: [item],
    page: 0,
    size: 100,
    totalElements: 1,
    totalPages: 1,
  }
}

describe('AppointmentFormDialog', () => {
  it('submits UTC time and displays booking conflicts', async () => {
    mockHttp('get').mockImplementation((url) => {
      if (url === '/api/customers') {
        return Promise.resolve(
          apiResponse(
            pageWith({
              id: 11,
              firstName: 'Alice',
              lastName: 'Murphy',
            }),
          ),
        )
      }
      if (url === '/api/services') {
        return Promise.resolve(
          apiResponse(
            pageWith({
              id: 22,
              name: 'Consultation',
              durationMinutes: 60,
            }),
          ),
        )
      }
      if (url === '/api/staff') {
        return Promise.resolve(
          apiResponse(
            pageWith({
              id: 33,
              firstName: 'Sam',
              lastName: 'Kelly',
            }),
          ),
        )
      }
      return Promise.reject(new Error(`Unexpected GET ${url}`))
    })
    const post = mockHttp('post').mockRejectedValueOnce(
      apiError('Appointment conflicts with an existing booking.', {
        status: 409,
        code: 'BOOKING_CONFLICT',
      }),
    )
    const onClose = vi.fn()
    const { user } = renderWithProviders(
      <AppointmentFormDialog
        appointment={null}
        onClose={onClose}
        open
      />,
      {
        authSession: {
          accessToken: 'owner-token',
          user: { email: 'owner@example.com', role: 'OWNER' },
        },
      },
    )

    await user.click(screen.getByRole('combobox', { name: 'Customer' }))
    await user.click(
      await screen.findByRole('option', { name: 'Alice Murphy' }),
    )
    await user.click(screen.getByRole('combobox', { name: 'Service' }))
    await user.click(
      await screen.findByRole('option', { name: 'Consultation (60 min)' }),
    )
    await user.click(screen.getByRole('combobox', { name: 'Staff' }))
    await user.click(
      await screen.findByRole('option', { name: 'Sam Kelly' }),
    )

    const localStartTime = '2026-09-12T14:00'
    fireEvent.change(screen.getByLabelText(/^Start date and time/), {
      target: { value: localStartTime },
    })
    await user.type(screen.getByLabelText('Notes'), 'First visit')
    await user.click(
      screen.getByRole('button', { name: 'Create appointment' }),
    )

    await waitFor(() =>
      expect(post).toHaveBeenCalledWith('/api/appointments', {
        customerId: 11,
        staffId: 33,
        serviceId: 22,
        startTime: new Date(localStartTime).toISOString(),
        notes: 'First visit',
      }),
    )
    expect(
      await screen.findByText(
        'This staff member already has an overlapping appointment.',
      ),
    ).toBeInTheDocument()
    expect(onClose).not.toHaveBeenCalled()
  })
})
