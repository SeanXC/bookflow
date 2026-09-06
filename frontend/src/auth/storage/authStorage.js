const AUTH_SESSION_KEY = 'bookflow.auth'

export const AUTH_UNAUTHORIZED_EVENT = 'bookflow:unauthorized'

const USER_ROLES = new Set(['OWNER', 'RECEPTIONIST', 'STAFF'])

/** @type {import('../types.js').AuthSession | null} */
let memorySession = null

/**
 * @param {unknown} value
 * @returns {value is import('../types.js').AuthSession}
 */
function isAuthSession(value) {
  if (!value || typeof value !== 'object') {
    return false
  }

  const session = /** @type {Record<string, unknown>} */ (value)
  const user =
    session.user && typeof session.user === 'object'
      ? /** @type {Record<string, unknown>} */ (session.user)
      : null

  return (
    typeof session.accessToken === 'string' &&
    session.accessToken.length > 0 &&
    user !== null &&
    typeof user.email === 'string' &&
    typeof user.role === 'string' &&
    USER_ROLES.has(user.role)
  )
}

/**
 * @returns {import('../types.js').AuthSession | null}
 */
export function readAuthSession() {
  if (memorySession) {
    return memorySession
  }

  try {
    const storedValue = window.sessionStorage.getItem(AUTH_SESSION_KEY)
    if (!storedValue) {
      return null
    }

    const parsedValue = JSON.parse(storedValue)
    if (isAuthSession(parsedValue)) {
      memorySession = parsedValue
      return memorySession
    }

    window.sessionStorage.removeItem(AUTH_SESSION_KEY)
  } catch {
    return null
  }

  return null
}

/**
 * @param {import('../types.js').AuthSession} session
 */
export function writeAuthSession(session) {
  memorySession = session
  try {
    window.sessionStorage.setItem(AUTH_SESSION_KEY, JSON.stringify(session))
  } catch {
    // Authentication remains available in memory when storage is unavailable.
  }
}

export function clearAuthSession() {
  memorySession = null
  try {
    window.sessionStorage.removeItem(AUTH_SESSION_KEY)
  } catch {
    // There is no persistent session to clear when storage is unavailable.
  }
}

export function getAccessToken() {
  return readAuthSession()?.accessToken ?? null
}
