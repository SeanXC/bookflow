import { beforeEach, describe, expect, it, vi } from 'vitest'

import {
  clearAuthSession,
  getAccessToken,
  readAuthSession,
  writeAuthSession,
} from './authStorage.js'

const session = {
  accessToken: 'owner-token',
  user: { email: 'owner@example.com', role: 'OWNER' },
}

describe('authStorage', () => {
  beforeEach(() => {
    clearAuthSession()
    window.sessionStorage.clear()
    vi.restoreAllMocks()
  })

  it('persists, reads, and clears valid sessions', () => {
    writeAuthSession(session)

    expect(readAuthSession()).toEqual(session)
    expect(getAccessToken()).toBe('owner-token')
    expect(window.sessionStorage.getItem('bookflow.auth')).toBe(
      JSON.stringify(session),
    )

    clearAuthSession()
    expect(readAuthSession()).toBeNull()
    expect(getAccessToken()).toBeNull()
  })

  it.each([
    ['not-json'],
    [JSON.stringify({ accessToken: '', user: session.user })],
    [JSON.stringify({ accessToken: 'token', user: { role: 'UNKNOWN' } })],
  ])('rejects malformed stored sessions', (storedValue) => {
    window.sessionStorage.setItem('bookflow.auth', storedValue)

    expect(readAuthSession()).toBeNull()
  })

  it('keeps the in-memory session when browser storage is unavailable', () => {
    vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
      throw new Error('Storage unavailable')
    })

    writeAuthSession(session)

    expect(readAuthSession()).toEqual(session)
  })

  it('still clears memory when persistent storage cannot be removed', () => {
    vi.spyOn(Storage.prototype, 'setItem').mockImplementation(() => {
      throw new Error('Storage unavailable')
    })
    vi.spyOn(Storage.prototype, 'removeItem').mockImplementation(() => {
      throw new Error('Storage unavailable')
    })
    writeAuthSession(session)

    clearAuthSession()

    expect(readAuthSession()).toBeNull()
  })
})
