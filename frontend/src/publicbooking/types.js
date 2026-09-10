/**
 * @typedef {object} PublicBusiness
 * @property {string} slug
 * @property {string} name
 * @property {string | null} phone
 * @property {string | null} description
 */

/**
 * @typedef {object} PublicService
 * @property {number} id
 * @property {string} name
 * @property {string | null} description
 * @property {number} price
 * @property {number} durationMinutes
 */

/**
 * @typedef {object} PublicStaff
 * @property {number} id
 * @property {string} firstName
 * @property {string} lastName
 */

/**
 * @typedef {object} PublicAvailableSlot
 * @property {string} startTime
 * @property {string} endTime
 */

/**
 * @typedef {object} PublicSlotFilters
 * @property {string} slug
 * @property {number} staffId
 * @property {number} serviceId
 * @property {string} from
 * @property {string} to
 */

/**
 * @typedef {object} PublicAppointmentRequest
 * @property {number} staffId
 * @property {number} serviceId
 * @property {string} startTime
 * @property {string} firstName
 * @property {string} lastName
 * @property {string} email
 * @property {string} phone
 * @property {string | null} notes
 */

/**
 * @typedef {object} PublicAppointment
 * @property {number} id
 * @property {PublicStaff} staff
 * @property {PublicService} service
 * @property {string} customerFirstName
 * @property {string} customerLastName
 * @property {string} startTime
 * @property {string} endTime
 * @property {'CONFIRMED' | 'COMPLETED' | 'CANCELLED'} status
 * @property {string | null} notes
 */

/**
 * @typedef {object} AssistantChatMessage
 * @property {'USER' | 'ASSISTANT'} role
 * @property {string} content
 */

/**
 * @typedef {object} AssistantProposal
 * @property {string} proposalId
 * @property {boolean} requiresConfirmation
 * @property {number} staffId
 * @property {string | null} staffFirstName
 * @property {string | null} staffLastName
 * @property {number} serviceId
 * @property {string | null} serviceName
 * @property {number | null} price
 * @property {number} durationMinutes
 * @property {string} startTime
 * @property {string} endTime
 * @property {string} firstName
 * @property {string} lastName
 * @property {string} email
 * @property {string} phone
 * @property {string | null} notes
 */

/**
 * @typedef {object} AssistantChatResponse
 * @property {string} message
 * @property {AssistantProposal | null} proposal
 */

/**
 * @typedef {object} AssistantBooking
 * @property {number} appointmentId
 * @property {number} staffId
 * @property {string} staffFirstName
 * @property {string} staffLastName
 * @property {number} serviceId
 * @property {string} serviceName
 * @property {string} startTime
 * @property {string} endTime
 * @property {'CONFIRMED' | 'COMPLETED' | 'CANCELLED'} status
 * @property {string} firstName
 * @property {string} lastName
 */

export {}
