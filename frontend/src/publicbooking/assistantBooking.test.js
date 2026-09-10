import { describe, expect, it } from 'vitest'

import { toPublicAppointment } from './assistantBooking.js'

describe('toPublicAppointment', () => {
  it('maps a confirmed assistant booking onto the public confirmation shape', () => {
    expect(
      toPublicAppointment(
        {
          appointmentId: 99,
          staffId: 40,
          staffFirstName: 'Anna',
          staffLastName: 'Smith',
          serviceId: 50,
          serviceName: 'Haircut',
          startTime: '2026-09-14T09:00:00Z',
          endTime: '2026-09-14T10:00:00Z',
          status: 'CONFIRMED',
          firstName: 'Emma',
          lastName: 'Chen',
        },
        {
          proposalId: 'prop-1',
          requiresConfirmation: true,
          staffId: 40,
          staffFirstName: 'Anna',
          staffLastName: 'Smith',
          serviceId: 50,
          serviceName: 'Haircut',
          price: 30,
          durationMinutes: 60,
          startTime: '2026-09-14T09:00:00Z',
          endTime: '2026-09-14T10:00:00Z',
          firstName: 'Emma',
          lastName: 'Chen',
          email: 'emma@example.com',
          phone: '555-0100',
          notes: 'Window seat',
        },
      ),
    ).toEqual({
      id: 99,
      staff: { id: 40, firstName: 'Anna', lastName: 'Smith' },
      service: {
        id: 50,
        name: 'Haircut',
        description: null,
        price: 30,
        durationMinutes: 60,
      },
      customerFirstName: 'Emma',
      customerLastName: 'Chen',
      startTime: '2026-09-14T09:00:00Z',
      endTime: '2026-09-14T10:00:00Z',
      status: 'CONFIRMED',
      notes: 'Window seat',
    })
  })
})
