import { useQuery } from '@tanstack/react-query'

import {
  getBookingsByWeek,
  getDashboardSummary,
  getRevenueByMonth,
} from './dashboardApi.js'

export const dashboardKeys = {
  all: ['dashboard'],
  summary: () => [...dashboardKeys.all, 'summary'],
  bookingsByWeek: () => [...dashboardKeys.all, 'bookings-by-week'],
  revenueByMonth: () => [...dashboardKeys.all, 'revenue-by-month'],
}

export function useDashboardSummary() {
  return useQuery({
    queryKey: dashboardKeys.summary(),
    queryFn: getDashboardSummary,
  })
}

export function useBookingsByWeek() {
  return useQuery({
    queryKey: dashboardKeys.bookingsByWeek(),
    queryFn: getBookingsByWeek,
  })
}

export function useRevenueByMonth() {
  return useQuery({
    queryKey: dashboardKeys.revenueByMonth(),
    queryFn: getRevenueByMonth,
  })
}
