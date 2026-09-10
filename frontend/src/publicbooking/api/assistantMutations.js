import { useMutation, useQueryClient } from '@tanstack/react-query'

import { publicBookingKeys } from './publicBookingQueries.js'
import {
  chatPublicAssistant,
  confirmPublicAssistantBooking,
} from './assistantApi.js'

/**
 * @param {string} slug
 */
export function useChatPublicAssistant(slug) {
  return useMutation({
    mutationFn: (
      /** @type {import('../types.js').AssistantChatMessage[]} */ messages,
    ) => chatPublicAssistant(slug, messages),
  })
}

/**
 * @param {string} slug
 */
export function useConfirmPublicAssistantBooking(slug) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (/** @type {string} */ proposalId) =>
      confirmPublicAssistantBooking(slug, proposalId),
    onSuccess: () =>
      queryClient.invalidateQueries({
        queryKey: publicBookingKeys.slotsRoot(slug),
      }),
  })
}
