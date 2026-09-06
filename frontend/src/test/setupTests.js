import '@testing-library/jest-dom/vitest'
import { cleanup } from '@testing-library/react'
import { afterEach } from 'vitest'

import { clearAuthSession } from '../auth/storage/authStorage.js'

afterEach(() => {
  cleanup()
  clearAuthSession()
})
