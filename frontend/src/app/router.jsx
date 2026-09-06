import { createBrowserRouter, Navigate } from 'react-router-dom'

import AppLayout from '../layout/AppLayout.jsx'
import ProtectedRoute from '../layout/ProtectedRoute.jsx'
import HomeRedirect from './HomeRedirect.jsx'
import {
  AppointmentListPage,
  CustomerDetailPage,
  CustomerListPage,
  DashboardPage,
  LoginPage,
  RegisterPage,
  ServiceListPage,
  StaffListPage,
  UserListPage,
} from './lazyPages.jsx'

export const router = createBrowserRouter([
  {
    element: <ProtectedRoute />,
    children: [
      {
        element: <AppLayout />,
        children: [
          {
            index: true,
            element: <HomeRedirect />,
          },
          {
            element: <ProtectedRoute allowedRoles={['OWNER']} />,
            children: [
              {
                path: 'dashboard',
                element: <DashboardPage />,
              },
              {
                path: 'users',
                element: <UserListPage />,
              },
            ],
          },
          {
            path: 'appointments',
            element: <AppointmentListPage />,
          },
          {
            element: (
              <ProtectedRoute allowedRoles={['OWNER', 'RECEPTIONIST']} />
            ),
            children: [
              {
                path: 'customers',
                element: <CustomerListPage />,
              },
              {
                path: 'customers/:customerId',
                element: <CustomerDetailPage />,
              },
              {
                path: 'staff',
                element: <StaffListPage />,
              },
            ],
          },
          {
            path: 'services',
            element: <ServiceListPage />,
          },
          {
            path: '*',
            element: <Navigate replace to="/" />,
          },
        ],
      },
    ],
  },
  {
    path: '/login',
    element: <LoginPage />,
  },
  {
    path: '/register',
    element: <RegisterPage />,
  },
])
