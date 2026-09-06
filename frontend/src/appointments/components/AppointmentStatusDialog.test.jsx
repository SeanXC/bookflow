import { describe, expect, it, vi } from 'vitest'

import { apiResponse, mockHttp } from '../../test/httpMock.js'
import {
  renderWithProviders,
  screen,
  waitFor,
} from '../../test/testUtils.jsx'
import AppointmentStatusDialog from './AppointmentStatusDialog.jsx'

describe('AppointmentStatusDialog', () => {
  it('confirms a completed appointment status update', async () => {
    const appointment = {
      id: 42,
      customer: {
        firstName: 'Alice',
        lastName: 'Murphy',
      },
    }
    const patch = mockHttp('patch').mockResolvedValueOnce(
      apiResponse({
        ...appointment,
        status: 'COMPLETED',
      }),
    )
    const onClose = vi.fn()
    const { user } = renderWithProviders(
      <AppointmentStatusDialog
        appointment={appointment}
        onClose={onClose}
        status="COMPLETED"
      />,
      {
        authSession: {
          accessToken: 'staff-token',
          user: { email: 'staff@example.com', role: 'STAFF' },
        },
      },
    )

    expect(screen.getByText('Alice Murphy')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Complete' }))

    await waitFor(() =>
      expect(patch).toHaveBeenCalledWith('/api/appointments/42/status', {
        status: 'COMPLETED',
      }),
    )
    expect(onClose).toHaveBeenCalledOnce()
  })
})
