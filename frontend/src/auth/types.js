/**
 * @typedef {'OWNER' | 'RECEPTIONIST' | 'STAFF'} UserRole
 */

/**
 * @typedef {object} AuthenticatedUser
 * @property {string} email
 * @property {UserRole} role
 */

/**
 * @typedef {object} AuthSession
 * @property {string} accessToken
 * @property {AuthenticatedUser} user
 */

/**
 * @typedef {object} LoginRequest
 * @property {string} email
 * @property {string} password
 */

/**
 * @typedef {object} RegisterRequest
 * @property {string} businessName
 * @property {string} businessEmail
 * @property {string} [businessPhone]
 * @property {string} ownerEmail
 * @property {string} password
 */

export {}
