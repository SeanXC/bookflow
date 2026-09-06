import { describe, expect, it, vi } from 'vitest'

import { apiResponse, mockHttp } from '../../test/httpMock.js'
import {
  renderWithProviders,
  screen,
  waitFor,
} from '../../test/testUtils.jsx'
import UserEnabledDialog from './UserEnabledDialog.jsx'

describe('UserEnabledDialog', () => {
  it('confirms disabling an enabled account', async () => {
    const managedUser = {
      id: 18,
      email: 'staff@example.com',
      role: 'STAFF',
      enabled: true,
      createdAt: '2026-09-06T20:00:00Z',
    }
    const patch = mockHttp('patch').mockResolvedValueOnce(
      apiResponse({ ...managedUser, enabled: false }),
    )
    const onClose = vi.fn()
    const { user } = renderWithProviders(
      <UserEnabledDialog onClose={onClose} user={managedUser} />,
      {
        authSession: {
          accessToken: 'owner-token',
          user: { email: 'owner@example.com', role: 'OWNER' },
        },
      },
    )

    expect(screen.getByText('staff@example.com')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Disable' }))

    await waitFor(() =>
      expect(patch).toHaveBeenCalledWith('/api/users/18/enabled', {
        enabled: false,
      }),
    )
    expect(onClose).toHaveBeenCalledOnce()
  })
})
