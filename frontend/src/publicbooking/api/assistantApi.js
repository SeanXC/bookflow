import { httpClient } from '../../api/httpClient.js'
import { ASSISTANT_CHAT_TIMEOUT_MS } from '../constants.js'

/**
 * @param {string} slug
 * @param {import('../types.js').AssistantChatMessage[]} messages
 * @returns {Promise<import('../types.js').AssistantChatResponse>}
 */
export async function chatPublicAssistant(slug, messages) {
  const response = await httpClient.post(
    `/api/public/businesses/${slug}/assistant`,
    { messages },
    { timeout: ASSISTANT_CHAT_TIMEOUT_MS },
  )
  return response.data
}

/**
 * @param {string} slug
 * @param {string} proposalId
 * @returns {Promise<import('../types.js').AssistantBooking>}
 */
export async function confirmPublicAssistantBooking(slug, proposalId) {
  const response = await httpClient.post(
    `/api/public/businesses/${slug}/assistant/confirm`,
    { proposalId },
  )
  return response.data
}
