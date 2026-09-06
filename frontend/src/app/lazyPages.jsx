import { lazy } from 'react'

export const AppointmentListPage = lazy(
  () => import('../appointments/pages/AppointmentListPage.jsx'),
)
export const CustomerListPage = lazy(
  () => import('../customers/pages/CustomerListPage.jsx'),
)
export const CustomerDetailPage = lazy(
  () => import('../customers/pages/CustomerDetailPage.jsx'),
)
export const DashboardPage = lazy(
  () => import('../dashboard/pages/DashboardPage.jsx'),
)
export const LoginPage = lazy(() => import('../auth/pages/LoginPage.jsx'))
export const RegisterPage = lazy(() => import('../auth/pages/RegisterPage.jsx'))
export const ServiceListPage = lazy(
  () => import('../services/pages/ServiceListPage.jsx'),
)
export const StaffListPage = lazy(
  () => import('../staff/pages/StaffListPage.jsx'),
)
