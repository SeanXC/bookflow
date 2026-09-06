/**
 * @typedef {object} ManagedUser
 * @property {number} id
 * @property {string} email
 * @property {import('../auth/types.js').UserRole} role
 * @property {boolean} enabled
 * @property {string} createdAt
 */

/**
 * @typedef {object} ManagedUserRequest
 * @property {string} email
 * @property {string} password
 * @property {'RECEPTIONIST' | 'STAFF'} role
 */

export {}
