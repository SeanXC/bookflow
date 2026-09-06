export const DRAWER_WIDTH = 256

export const navigationItems = [
  {
    label: 'Dashboard',
    path: '/dashboard',
    icon: 'dashboard',
    roles: ['OWNER'],
  },
  {
    label: 'Appointments',
    staffLabel: 'My schedule',
    path: '/appointments',
    icon: 'appointments',
    roles: ['OWNER', 'RECEPTIONIST', 'STAFF'],
  },
  {
    label: 'Customers',
    path: '/customers',
    icon: 'customers',
    roles: ['OWNER', 'RECEPTIONIST'],
  },
  {
    label: 'Staff',
    path: '/staff',
    icon: 'staff',
    roles: ['OWNER', 'RECEPTIONIST'],
  },
  {
    label: 'Services',
    path: '/services',
    icon: 'services',
    roles: ['OWNER', 'RECEPTIONIST', 'STAFF'],
  },
  {
    label: 'Users',
    path: '/users',
    icon: 'users',
    roles: ['OWNER'],
  },
]
