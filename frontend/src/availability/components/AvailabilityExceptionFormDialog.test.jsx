import { describe, expect, it, vi } from 'vitest'

import { apiResponse, mockHttp } from '../../test/httpMock.js'
import {
  fireEvent,
  renderWithProviders,
  screen,
  waitFor,
} from '../../test/testUtils.jsx'
import AvailabilityExceptionFormDialog from './AvailabilityExceptionFormDialog.jsx'

const ownerSession = {
  accessToken: 'owner-token',
  user: { email: 'owner@example.com', role: 'OWNER' },
}

describe('AvailabilityExceptionFormDialog', () => {
  it('creates an all-day unavailability without times', async () => {
    const post = mockHttp('post').mockResolvedValueOnce(
      apiResponse({
        id: 9,
        staffId: 40,
        exceptionDate: '2026-09-14',
        type: 'UNAVAILABLE',
        startTime: null,
        endTime: null,
        note: 'Holiday',
      }),
    )
    const onClose = vi.fn()
    const { user } = renderWithProviders(
      <AvailabilityExceptionFormDialog
        exception={null}
        onClose={onClose}
        staffId={40}
      />,
      { authSession: ownerSession },
    )

    fireEvent.change(screen.getByLabelText(/^Date/), {
      target: { value: '2026-09-14' },
    })
    await user.type(screen.getByLabelText(/^Note/), 'Holiday')
    await user.click(screen.getByRole('button', { name: 'Save' }))

    await waitFor(() =>
      expect(post).toHaveBeenCalledWith(
        '/api/staff/40/availability/exceptions',
        {
          exceptionDate: '2026-09-14',
          type: 'UNAVAILABLE',
          startTime: null,
          endTime: null,
          note: 'Holiday',
        },
      ),
    )
    expect(onClose).toHaveBeenCalledOnce()
  })

  it('creates custom hours with a time window', async () => {
    const post = mockHttp('post').mockResolvedValueOnce(
      apiResponse({
        id: 10,
        staffId: 40,
        exceptionDate: '2026-09-14',
        type: 'CUSTOM_HOURS',
        startTime: '13:00:00',
        endTime: '15:00:00',
        note: null,
      }),
    )
    const onClose = vi.fn()
    const { user } = renderWithProviders(
      <AvailabilityExceptionFormDialog
        exception={null}
        onClose={onClose}
        staffId={40}
      />,
      { authSession: ownerSession },
    )

    fireEvent.change(screen.getByLabelText(/^Date/), {
      target: { value: '2026-09-14' },
    })
    await user.click(screen.getByRole('combobox', { name: 'Type' }))
    await user.click(
      await screen.findByRole('option', { name: 'Custom hours' }),
    )
    fireEvent.change(screen.getByLabelText(/^Start time/), {
      target: { value: '13:00' },
    })
    fireEvent.change(screen.getByLabelText(/^End time/), {
      target: { value: '15:00' },
    })
    await user.click(screen.getByRole('button', { name: 'Save' }))

    await waitFor(() =>
      expect(post).toHaveBeenCalledWith(
        '/api/staff/40/availability/exceptions',
        {
          exceptionDate: '2026-09-14',
          type: 'CUSTOM_HOURS',
          startTime: '13:00:00',
          endTime: '15:00:00',
          note: null,
        },
      ),
    )
    expect(onClose).toHaveBeenCalledOnce()
  })
})
