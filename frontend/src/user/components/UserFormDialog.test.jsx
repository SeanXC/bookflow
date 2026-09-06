import { describe, expect, it, vi } from 'vitest'

import { apiResponse, mockHttp } from '../../test/httpMock.js'
import {
  renderWithProviders,
  screen,
  waitFor,
} from '../../test/testUtils.jsx'
import UserFormDialog from './UserFormDialog.jsx'

describe('UserFormDialog', () => {
  it('creates a normalized Receptionist account', async () => {
    const post = mockHttp('post').mockResolvedValueOnce(
      apiResponse({
        id: 12,
        email: 'reception@example.com',
        role: 'RECEPTIONIST',
        enabled: true,
        createdAt: '2026-09-06T20:00:00Z',
      }),
    )
    const onClose = vi.fn()
    const { user } = renderWithProviders(
      <UserFormDialog onClose={onClose} />,
      {
        authSession: {
          accessToken: 'owner-token',
          user: { email: 'owner@example.com', role: 'OWNER' },
        },
      },
    )

    await user.type(
      screen.getByLabelText(/^Email address/),
      'RECEPTION@EXAMPLE.COM',
    )
    await user.type(screen.getByLabelText(/^Password/), 'DemoPass123!')
    await user.click(screen.getByRole('combobox', { name: /^Role/ }))
    await user.click(
      await screen.findByRole('option', { name: 'Receptionist' }),
    )
    await user.click(screen.getByRole('button', { name: 'Create account' }))

    await waitFor(() =>
      expect(post).toHaveBeenCalledWith('/api/users', {
        email: 'reception@example.com',
        password: 'DemoPass123!',
        role: 'RECEPTIONIST',
      }),
    )
    expect(onClose).toHaveBeenCalledOnce()
  })
})
