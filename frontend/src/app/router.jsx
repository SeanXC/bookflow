import { createBrowserRouter, Navigate } from 'react-router-dom'

import LoginPage from '../auth/pages/LoginPage.jsx'
import RegisterPage from '../auth/pages/RegisterPage.jsx'
import AppLayout from '../layout/AppLayout.jsx'
import ProtectedRoute from '../layout/ProtectedRoute.jsx'
import PlaceholderPage from '../shared/components/PlaceholderPage.jsx'
import StaffListPage from '../staff/pages/StaffListPage.jsx'
import HomeRedirect from './HomeRedirect.jsx'

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
                element: (
                  <PlaceholderPage
                    description="Monitor bookings, revenue, customers, and cancellations."
                    title="Dashboard"
                  />
                ),
              },
              {
                path: 'users',
                element: (
                  <PlaceholderPage
                    description="Manage Receptionist and Staff login accounts."
                    title="Users"
                  />
                ),
              },
            ],
          },
          {
            path: 'appointments',
            element: (
              <PlaceholderPage
                description="Review and manage the tenant appointment schedule."
                title="Appointments"
              />
            ),
          },
          {
            element: (
              <ProtectedRoute allowedRoles={['OWNER', 'RECEPTIONIST']} />
            ),
            children: [
              {
                path: 'customers',
                element: (
                  <PlaceholderPage
                    description="Maintain customer details and appointment history."
                    title="Customers"
                  />
                ),
              },
              {
                path: 'staff',
                element: <StaffListPage />,
              },
            ],
          },
          {
            path: 'services',
            element: (
              <PlaceholderPage
                description="Browse the services available for booking."
                title="Services"
              />
            ),
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
