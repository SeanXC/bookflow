import { describe, expect, it, vi } from 'vitest'

import { apiError, apiResponse, mockHttp } from '../../test/httpMock.js'
import {
  fireEvent,
  renderWithProviders,
  screen,
  waitFor,
} from '../../test/testUtils.jsx'
import WeeklyHoursFormDialog from './WeeklyHoursFormDialog.jsx'

const ownerSession = {
  accessToken: 'owner-token',
  user: { email: 'owner@example.com', role: 'OWNER' },
}

describe('WeeklyHoursFormDialog', () => {
  it('creates weekly hours with API time values', async () => {
    const post = mockHttp('post').mockResolvedValueOnce(
      apiResponse({
        id: 7,
        staffId: 40,
        dayOfWeek: 'SATURDAY',
        startTime: '10:00:00',
        endTime: '14:00:00',
      }),
    )
    const onClose = vi.fn()
    const { user } = renderWithProviders(
      <WeeklyHoursFormDialog hours={null} onClose={onClose} staffId={40} />,
      { authSession: ownerSession },
    )

    await user.click(screen.getByRole('combobox', { name: 'Day' }))
    await user.click(await screen.findByRole('option', { name: 'Saturday' }))
    fireEvent.change(screen.getByLabelText(/^Start time/), {
      target: { value: '10:00' },
    })
    fireEvent.change(screen.getByLabelText(/^End time/), {
      target: { value: '14:00' },
    })
    await user.click(screen.getByRole('button', { name: 'Save' }))

    await waitFor(() =>
      expect(post).toHaveBeenCalledWith(
        '/api/staff/40/availability/weekly-hours',
        {
          dayOfWeek: 'SATURDAY',
          startTime: '10:00:00',
          endTime: '14:00:00',
        },
      ),
    )
    expect(onClose).toHaveBeenCalledOnce()
  })

  it('blocks inverted times and shows overlap errors', async () => {
    const post = mockHttp('post').mockRejectedValueOnce(
      apiError('Weekly hours cannot overlap on the same day', {
        status: 400,
        code: 'INVALID_OPERATION',
      }),
    )
    const onClose = vi.fn()
    const { user } = renderWithProviders(
      <WeeklyHoursFormDialog hours={null} onClose={onClose} staffId={40} />,
      { authSession: ownerSession },
    )

    fireEvent.change(screen.getByLabelText(/^End time/), {
      target: { value: '08:00' },
    })
    await user.click(screen.getByRole('button', { name: 'Save' }))

    expect(
      await screen.findByText('End time must be after start time.'),
    ).toBeInTheDocument()
    expect(post).not.toHaveBeenCalled()

    fireEvent.change(screen.getByLabelText(/^End time/), {
      target: { value: '17:00' },
    })
    await user.click(screen.getByRole('button', { name: 'Save' }))

    expect(
      await screen.findByText('Weekly hours cannot overlap on the same day'),
    ).toBeInTheDocument()
    expect(onClose).not.toHaveBeenCalled()
  })
})
