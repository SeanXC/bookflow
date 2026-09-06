import { lazy } from 'react'

export const CustomerListPage = lazy(
  () => import('../customers/pages/CustomerListPage.jsx'),
)
export const LoginPage = lazy(() => import('../auth/pages/LoginPage.jsx'))
export const RegisterPage = lazy(() => import('../auth/pages/RegisterPage.jsx'))
export const ServiceListPage = lazy(
  () => import('../services/pages/ServiceListPage.jsx'),
)
export const StaffListPage = lazy(
  () => import('../staff/pages/StaffListPage.jsx'),
)
