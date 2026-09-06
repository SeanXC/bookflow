import { Navigate } from 'react-router-dom'

import { useAuth } from '../auth/context/useAuth.js'

function HomeRedirect() {
  const { user } = useAuth()
  const destination = user?.role === 'OWNER' ? '/dashboard' : '/appointments'

  return <Navigate replace to={destination} />
}

export default HomeRedirect
