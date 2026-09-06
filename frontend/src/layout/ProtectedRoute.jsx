import PropTypes from 'prop-types'
import { Navigate, Outlet, useLocation } from 'react-router-dom'

import { useAuth } from '../auth/context/useAuth.js'

function ProtectedRoute({ allowedRoles }) {
  const { isAuthenticated, user } = useAuth()
  const location = useLocation()

  if (!isAuthenticated) {
    return <Navigate replace state={{ from: location }} to="/login" />
  }

  if (allowedRoles && user && !allowedRoles.includes(user.role)) {
    return <Navigate replace to="/" />
  }

  return <Outlet />
}

ProtectedRoute.propTypes = {
  allowedRoles: PropTypes.arrayOf(
    PropTypes.oneOf(['OWNER', 'RECEPTIONIST', 'STAFF']).isRequired,
  ),
}

export default ProtectedRoute
