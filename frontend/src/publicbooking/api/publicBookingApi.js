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
