import { describe, expect, it } from 'vitest'

import { MAX_SLOT_RANGE_DAYS } from './constants.js'
import {
  getSlotRangeError,
  groupSlotsByDay,
} from './format.js'

describe('public booking format', () => {
  it('rejects an inverted or oversized slot range', () => {
    expect(getSlotRangeError('', '2026-09-14')).toBe(
      'Choose a start and end date.',
    )
    expect(getSlotRangeError('2026-09-15', '2026-09-14')).toBe(
      'The start date must not be after the end date.',
    )
    expect(getSlotRangeError('2026-09-01', '2026-09-20')).toBe(
      `The date range cannot exceed ${MAX_SLOT_RANGE_DAYS} days.`,
    )
    expect(getSlotRangeError('2026-09-14', '2026-09-27')).toBeNull()
  })

  it('groups slots by local date', () => {
    const groups = groupSlotsByDay([
      {
        startTime: '2026-09-14T09:00:00Z',
        endTime: '2026-09-14T10:00:00Z',
      },
      {
        startTime: '2026-09-14T10:00:00Z',
        endTime: '2026-09-14T11:00:00Z',
      },
      {
        startTime: '2026-09-15T09:00:00Z',
        endTime: '2026-09-15T10:00:00Z',
      },
    ])

    expect(groups).toHaveLength(2)
    expect(groups[0][1]).toHaveLength(2)
    expect(groups[1][1]).toHaveLength(1)
  })
})
