/**
 * @param {import('./types.js').AssistantBooking} booking
 * @param {import('./types.js').AssistantProposal | null} [proposal]
 * @returns {import('./types.js').PublicAppointment}
 */
export function toPublicAppointment(booking, proposal = null) {
  return {
    id: booking.appointmentId,
    staff: {
      id: booking.staffId,
      firstName: booking.staffFirstName,
      lastName: booking.staffLastName,
    },
    service: {
      id: booking.serviceId,
      name: booking.serviceName,
      description: null,
      price: proposal?.price ?? 0,
      durationMinutes: proposal?.durationMinutes ?? 0,
    },
    customerFirstName: booking.firstName,
    customerLastName: booking.lastName,
    startTime: booking.startTime,
    endTime: booking.endTime,
    status: booking.status,
    notes: proposal?.notes ?? null,
  }
}
