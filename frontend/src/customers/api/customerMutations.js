import { useMutation, useQueryClient } from '@tanstack/react-query'

import { createCustomer, updateCustomer } from './customerApi.js'
import { customerKeys } from './customerQueries.js'

/**
 * @param {{
 *   customerId: number,
 *   request: import('../types.js').CustomerRequest
 * }} variables
 */
function updateCustomerMutation({ customerId, request }) {
  return updateCustomer(customerId, request)
}

export function useCreateCustomer() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: createCustomer,
    onSuccess: () =>
      queryClient.invalidateQueries({ queryKey: customerKeys.all }),
  })
}

export function useUpdateCustomer() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: updateCustomerMutation,
    onSuccess: (customer) => {
      queryClient.setQueryData(customerKeys.detail(customer.id), customer)
      return queryClient.invalidateQueries({ queryKey: customerKeys.all })
    },
  })
}
