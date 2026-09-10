import { httpClient } from '../../api/httpClient.js'

/**
 * @param {string} slug
 * @returns {Promise<import('../types.js').PublicBusiness>}
 */
export async function getPublicBusiness(slug) {
  const response = await httpClient.get(`/api/public/businesses/${slug}`)
  return response.data
}

/**
 * @param {string} slug
 * @returns {Promise<import('../types.js').PublicService[]>}
 */
export async function getPublicServices(slug) {
  const response = await httpClient.get(
    `/api/public/businesses/${slug}/services`,
  )
  return response.data
}

/**
 * @param {string} slug
 * @returns {Promise<import('../types.js').PublicStaff[]>}
 */
export async function getPublicStaff(slug) {
  const response = await httpClient.get(`/api/public/businesses/${slug}/staff`)
  return response.data
}

/**
 * @param {import('../types.js').PublicSlotFilters} filters
 * @returns {Promise<import('../types.js').PublicAvailableSlot[]>}
 */
export async function getPublicSlots(filters) {
  const response = await httpClient.get(
    `/api/public/businesses/${filters.slug}/staff/${filters.staffId}/slots`,
    {
      params: {
        serviceId: filters.serviceId,
        from: filters.from,
        to: filters.to,
      },
    },
  )
  return response.data
}

/**
 * @param {string} slug
 * @param {import('../types.js').PublicAppointmentRequest} request
 * @returns {Promise<import('../types.js').PublicAppointment>}
 */
export async function createPublicAppointment(slug, request) {
  const response = await httpClient.post(
    `/api/public/businesses/${slug}/appointments`,
    request,
  )
  return response.data
}
