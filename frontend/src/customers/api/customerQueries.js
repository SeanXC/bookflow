import { keepPreviousData, useQuery } from '@tanstack/react-query'

import {
  getCustomer,
  getCustomerAppointments,
  getCustomers,
} from './customerApi.js'

export const customerKeys = {
  all: ['customers'],
  /** @param {Parameters<typeof getCustomers>[0]} filters */
  list: (filters) => [...customerKeys.all, 'list', filters],
  /** @param {number} customerId */
  detail: (customerId) => [...customerKeys.all, 'detail', customerId],
  /**
   * @param {number} customerId
   * @param {{ page: number, pageSize: number }} page
   */
  history: (customerId, page) => [
    ...customerKeys.detail(customerId),
    'appointments',
    page,
  ],
}

/**
 * @param {Parameters<typeof getCustomers>[0]} options
 */
export function useCustomers(options) {
  return useQuery({
    queryKey: customerKeys.list(options),
    queryFn: () => getCustomers(options),
    placeholderData: keepPreviousData,
  })
}

/**
 * @param {number} customerId
 * @param {boolean} [enabled]
 */
export function useCustomer(customerId, enabled = true) {
  return useQuery({
    enabled,
    queryKey: customerKeys.detail(customerId),
    queryFn: () => getCustomer(customerId),
  })
}

/**
 * @param {number} customerId
 * @param {{ page: number, pageSize: number }} page
 * @param {boolean} [enabled]
 */
export function useCustomerAppointments(customerId, page, enabled = true) {
  return useQuery({
    enabled,
    queryKey: customerKeys.history(customerId, page),
    queryFn: () => getCustomerAppointments(customerId, page),
    placeholderData: keepPreviousData,
  })
}
