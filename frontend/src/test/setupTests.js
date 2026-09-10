import '@testing-library/jest-dom/vitest'
import { cleanup, configure } from '@testing-library/react'
import { afterEach } from 'vitest'

import { clearAuthSession } from '../auth/storage/authStorage.js'

configure({ asyncUtilTimeout: 5000 })

if (!globalThis.ResizeObserver) {
  globalThis.ResizeObserver = class ResizeObserver {
    observe() {}
    unobserve() {}
    disconnect() {}
  }
}

afterEach(() => {
  cleanup()
  clearAuthSession()
})
