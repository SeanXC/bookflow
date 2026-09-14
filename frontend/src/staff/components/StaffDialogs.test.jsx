import { describe, expect, it, vi } from 'vitest'

import { apiError, apiResponse, mockHttp } from '../../test/httpMock.js'
import {
  renderWithProviders,
  screen,
  waitFor,
} from '../../test/testUtils.jsx'
import DeactivateStaffDialog from './DeactivateStaffDialog.jsx'
import StaffFormDialog from './StaffFormDialog.jsx'

const ownerSession = {
  accessToken: 'owner-token',
  user: { email: 'owner@example.com', role: 'OWNER' },
}

const users = [
  {
    id: 7,
    email: 'anna@example.com',
    role: 'STAFF',
    enabled: true,
  },
  {
    id: 8,
    email: 'disabled@example.com',
    role: 'STAFF',
    enabled: false,
  },
  {
    id: 9,
    email: 'reception@example.com',
    role: 'RECEPTIONIST',
    enabled: true,
  },
]

const staff = {
  id: 40,
  userId: 8,
  firstName: 'Anna',
  lastName: 'Smith',
  phone: '0871234567',
  active: true,
}

describe('Staff dialogs', () => {
  it('creates a normalized staff member linked to an enabled staff account', async () => {
    const post = mockHttp('post').mockResolvedValueOnce(
      apiResponse({ ...staff, userId: 7 }),
    )
    const onClose = vi.fn()
    const { user } = renderWithProviders(
      <StaffFormDialog
        onClose={onClose}
        open
        staff={null}
        users={users}
      />,
      { authSession: ownerSession },
    )

    await user.type(screen.getByLabelText(/^First name/), '  Anna  ')
    await user.type(screen.getByLabelText(/^Last name/), '  Smith  ')
    await user.type(screen.getByLabelText('Phone (optional)'), '  0871234567  ')
    await user.click(screen.getByRole('combobox', { name: 'Login account' }))
    await user.click(
      await screen.findByRole('option', { name: 'anna@example.com' }),
    )
    await user.click(screen.getByRole('button', { name: 'Save' }))

    await waitFor(() =>
      expect(post).toHaveBeenCalledWith('/api/staff', {
        userId: 7,
        firstName: 'Anna',
        lastName: 'Smith',
        phone: '0871234567',
      }),
    )
    expect(onClose).toHaveBeenCalledOnce()
    expect(screen.queryByText('reception@example.com')).not.toBeInTheDocument()
  })

  it('updates staff and keeps the linked disabled account selectable', async () => {
    const put = mockHttp('put').mockResolvedValueOnce(apiResponse(staff))
    const onClose = vi.fn()
    const { user } = renderWithProviders(
      <StaffFormDialog onClose={onClose} open staff={staff} users={users} />,
      { authSession: ownerSession },
    )

    await user.click(screen.getByRole('combobox', { name: 'Login account' }))
    expect(
      await screen.findByRole('option', {
        name: 'disabled@example.com (disabled)',
      }),
    ).toBeInTheDocument()
    await user.click(
      screen.getByRole('option', { name: 'No linked account' }),
    )
    await user.clear(screen.getByLabelText('Phone (optional)'))
    await user.click(screen.getByRole('button', { name: 'Save' }))

    await waitFor(() =>
      expect(put).toHaveBeenCalledWith('/api/staff/40', {
        userId: null,
        firstName: 'Anna',
        lastName: 'Smith',
        phone: null,
      }),
    )
    expect(onClose).toHaveBeenCalledOnce()
  })

  it('displays staff save errors', async () => {
    mockHttp('post').mockRejectedValueOnce(
      apiError('Unable to save staff.', { status: 400 }),
    )
    const onClose = vi.fn()
    const { user } = renderWithProviders(
      <StaffFormDialog onClose={onClose} open staff={null} users={users} />,
      { authSession: ownerSession },
    )

    await user.type(screen.getByLabelText(/^First name/), 'Anna')
    await user.type(screen.getByLabelText(/^Last name/), 'Smith')
    await user.click(screen.getByRole('button', { name: 'Save' }))

    expect(await screen.findByText('Unable to save staff.')).toBeInTheDocument()
    expect(onClose).not.toHaveBeenCalled()
  })

  it('deactivates a staff member', async () => {
    const del = mockHttp('delete').mockResolvedValueOnce(apiResponse(null))
    const onClose = vi.fn()
    const { user } = renderWithProviders(
      <DeactivateStaffDialog onClose={onClose} staff={staff} />,
      { authSession: ownerSession },
    )

    expect(
      screen.getByText(
        'Anna Smith will no longer be available for new appointments.',
      ),
    ).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Deactivate' }))

    await waitFor(() => expect(del).toHaveBeenCalledWith('/api/staff/40'))
    expect(onClose).toHaveBeenCalledOnce()
  })

  it('keeps the deactivation dialog open after API errors', async () => {
    mockHttp('delete').mockRejectedValueOnce(
      apiError('Unable to deactivate staff.', { status: 409 }),
    )
    const onClose = vi.fn()
    const { user } = renderWithProviders(
      <DeactivateStaffDialog onClose={onClose} staff={staff} />,
      { authSession: ownerSession },
    )

    await user.click(screen.getByRole('button', { name: 'Deactivate' }))

    expect(
      await screen.findByText('Unable to deactivate staff.'),
    ).toBeInTheDocument()
    expect(onClose).not.toHaveBeenCalled()
  })
})
