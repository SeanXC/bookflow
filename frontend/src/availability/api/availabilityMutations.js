import { useMutation, useQueryClient } from '@tanstack/react-query'

import {
  createAvailabilityException,
  createWeeklyHours,
  deleteAvailabilityException,
  deleteWeeklyHours,
  updateAvailabilityException,
  updateWeeklyHours,
} from './availabilityApi.js'
import { availabilityKeys } from './availabilityQueries.js'

/**
 * @param {{
 *   staffId: number,
 *   request: import('../types.js').WeeklyHoursRequest
 * }} variables
 */
function createWeeklyHoursMutation({ staffId, request }) {
  return createWeeklyHours(staffId, request)
}

/**
 * @param {{
 *   staffId: number,
 *   hoursId: number,
 *   request: import('../types.js').WeeklyHoursRequest
 * }} variables
 */
function updateWeeklyHoursMutation({ staffId, hoursId, request }) {
  return updateWeeklyHours(staffId, hoursId, request)
}

/**
 * @param {{ staffId: number, hoursId: number }} variables
 */
function deleteWeeklyHoursMutation({ staffId, hoursId }) {
  return deleteWeeklyHours(staffId, hoursId)
}

/**
 * @param {{
 *   staffId: number,
 *   request: import('../types.js').AvailabilityExceptionRequest
 * }} variables
 */
function createExceptionMutation({ staffId, request }) {
  return createAvailabilityException(staffId, request)
}

/**
 * @param {{
 *   staffId: number,
 *   exceptionId: number,
 *   request: import('../types.js').AvailabilityExceptionRequest
 * }} variables
 */
function updateExceptionMutation({ staffId, exceptionId, request }) {
  return updateAvailabilityException(staffId, exceptionId, request)
}

/**
 * @param {{ staffId: number, exceptionId: number }} variables
 */
function deleteExceptionMutation({ staffId, exceptionId }) {
  return deleteAvailabilityException(staffId, exceptionId)
}

function useAvailabilityMutationOptions() {
  const queryClient = useQueryClient()
  return {
    onSuccess: () =>
      queryClient.invalidateQueries({
        queryKey: availabilityKeys.all,
      }),
  }
}

export function useCreateWeeklyHours() {
  return useMutation({
    mutationFn: createWeeklyHoursMutation,
    ...useAvailabilityMutationOptions(),
  })
}

export function useUpdateWeeklyHours() {
  return useMutation({
    mutationFn: updateWeeklyHoursMutation,
    ...useAvailabilityMutationOptions(),
  })
}

export function useDeleteWeeklyHours() {
  return useMutation({
    mutationFn: deleteWeeklyHoursMutation,
    ...useAvailabilityMutationOptions(),
  })
}

export function useCreateAvailabilityException() {
  return useMutation({
    mutationFn: createExceptionMutation,
    ...useAvailabilityMutationOptions(),
  })
}

export function useUpdateAvailabilityException() {
  return useMutation({
    mutationFn: updateExceptionMutation,
    ...useAvailabilityMutationOptions(),
  })
}

export function useDeleteAvailabilityException() {
  return useMutation({
    mutationFn: deleteExceptionMutation,
    ...useAvailabilityMutationOptions(),
  })
}
