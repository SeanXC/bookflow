import { createContext } from 'react'

/**
 * @typedef {object} AuthContextValue
 * @property {import('../types.js').AuthenticatedUser | null} user
 * @property {boolean} isAuthenticated
 * @property {(request: import('../types.js').LoginRequest) =>
 *   Promise<import('../types.js').AuthenticatedUser>} login
 * @property {(request: import('../types.js').RegisterRequest) =>
 *   Promise<import('../types.js').AuthenticatedUser>} register
 * @property {() => void} logout
 */

/** @type {import('react').Context<AuthContextValue | null>} */
export const AuthContext = createContext(null)
