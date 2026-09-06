import { useCallback, useEffect, useMemo, useState } from 'react'
import PropTypes from 'prop-types'

import { queryClient } from '../../api/queryClient.js'
import {
  login as loginRequest,
  register as registerRequest,
} from '../api/authApi.js'
import {
  AUTH_UNAUTHORIZED_EVENT,
  clearAuthSession,
  readAuthSession,
  writeAuthSession,
} from '../storage/authStorage.js'
import { AuthContext } from './AuthContext.js'

function AuthProvider({ children }) {
  const [session, setSession] = useState(readAuthSession)

  const applySession = useCallback((nextSession) => {
    writeAuthSession(nextSession)
    setSession(nextSession)
    return nextSession.user
  }, [])

  const login = useCallback(
    async (request) => applySession(await loginRequest(request)),
    [applySession],
  )

  const register = useCallback(
    async (request) => applySession(await registerRequest(request)),
    [applySession],
  )

  const logout = useCallback(() => {
    clearAuthSession()
    setSession(null)
    queryClient.clear()
  }, [])

  useEffect(() => {
    window.addEventListener(AUTH_UNAUTHORIZED_EVENT, logout)
    return () => window.removeEventListener(AUTH_UNAUTHORIZED_EVENT, logout)
  }, [logout])

  const value = useMemo(
    () => ({
      user: session?.user ?? null,
      isAuthenticated: session !== null,
      login,
      register,
      logout,
    }),
    [login, logout, register, session],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

AuthProvider.propTypes = {
  children: PropTypes.node.isRequired,
}

export default AuthProvider
