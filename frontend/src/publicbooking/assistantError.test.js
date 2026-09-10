import { describe, expect, it } from 'vitest'

import { ApiError } from '../api/apiError.js'
import { getAssistantErrorMessage } from './assistantError.js'

describe('getAssistantErrorMessage', () => {
  it('maps conflict and rate-limit errors to guest-facing copy', () => {
    expect(
      getAssistantErrorMessage(
        new ApiError('This staff member already has an appointment.', {
          status: 409,
          code: 'BOOKING_CONFLICT',
        }),
      ),
    ).toBe('This time is no longer available. Please choose another slot.')
    expect(
      getAssistantErrorMessage(
        new ApiError('Too many assistant requests. Please try again later.', {
          status: 429,
          code: 'RATE_LIMITED',
        }),
      ),
    ).toBe('Too many assistant requests. Please try again later.')
    expect(
      getAssistantErrorMessage(
        new ApiError('The booking assistant is not configured.'),
      ),
    ).toBe('The booking assistant is not configured.')
  })
})
