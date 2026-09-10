import { describe, expect, it } from 'vitest'

import { ApiError } from '../api/apiError.js'
import { getPublicBookingErrorMessage } from './publicBookingError.js'

describe('getPublicBookingErrorMessage', () => {
  it('maps conflict and rate-limit errors to guest-facing copy', () => {
    expect(
      getPublicBookingErrorMessage(
        new ApiError('This staff member already has an appointment.', {
          status: 409,
          code: 'BOOKING_CONFLICT',
        }),
      ),
    ).toBe('This time is no longer available. Please choose another slot.')
    expect(
      getPublicBookingErrorMessage(
        new ApiError('Too many booking attempts. Please try again later.', {
          status: 429,
          code: 'RATE_LIMITED',
        }),
      ),
    ).toBe('Too many booking attempts. Please try again later.')
    expect(getPublicBookingErrorMessage(new Error('Slot closed'))).toBe(
      'Slot closed',
    )
  })
})
