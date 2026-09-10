import { describe, expect, it } from 'vitest'

import { getExceptionRangeError, toApiTime, toTimeInputValue } from './format.js'

describe('availability format helpers', () => {
  it('converts time values between inputs and the API', () => {
    expect(toTimeInputValue('09:00:00')).toBe('09:00')
    expect(toApiTime('09:00')).toBe('09:00:00')
  })

  it('rejects inverted or overly long exception ranges', () => {
    expect(getExceptionRangeError('2026-09-20', '2026-09-14')).toBe(
      'The start date must not be after the end date.',
    )
    expect(getExceptionRangeError('2026-09-01', '2026-10-02')).toBe(
      'The date range cannot exceed 31 days.',
    )
    expect(getExceptionRangeError('2026-09-01', '2026-10-01')).toBeNull()
  })
})
