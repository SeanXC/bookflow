import { describe, expect, it, vi } from 'vitest'

import { apiError, apiResponse, mockHttp } from '../../test/httpMock.js'
import {
  renderWithProviders,
  screen,
  waitFor,
} from '../../test/testUtils.jsx'
import CustomerFormDialog from './CustomerFormDialog.jsx'

const session = {
  accessToken: 'reception-token',
  user: { email: 'reception@example.com', role: 'RECEPTIONIST' },
}

const customer = {
  id: 5,
  firstName: 'Alice',
  lastName: 'Murphy',
  email: 'alice@example.com',
  phone: '0871234567',
  notes: 'Prefers afternoons',
}

describe('CustomerFormDialog', () => {
  it('creates a normalized customer with optional fields', async () => {
    const post = mockHttp('post').mockResolvedValueOnce(
      apiResponse({ ...customer, id: 6 }),
    )
    const onClose = vi.fn()
    const { user } = renderWithProviders(
      <CustomerFormDialog customer={null} onClose={onClose} />,
      { authSession: session },
    )

    await user.type(screen.getByLabelText(/^First name/), '  Ava  ')
    await user.type(screen.getByLabelText(/^Last name/), '  Walsh  ')
    await user.type(
      screen.getByLabelText('Email (optional)'),
      '  AVA@EXAMPLE.COM  ',
    )
    await user.type(screen.getByLabelText('Phone (optional)'), '  01234  ')
    await user.type(screen.getByLabelText('Notes (optional)'), '  New client  ')
    await user.click(screen.getByRole('button', { name: 'Save' }))

    await waitFor(() =>
      expect(post).toHaveBeenCalledWith('/api/customers', {
        firstName: 'Ava',
        lastName: 'Walsh',
        email: 'ava@example.com',
        phone: '01234',
        notes: 'New client',
      }),
    )
    expect(onClose).toHaveBeenCalledOnce()
  })

  it('updates a customer and converts cleared optional fields to null', async () => {
    const put = mockHttp('put').mockResolvedValueOnce(apiResponse(customer))
    const onClose = vi.fn()
    const { user } = renderWithProviders(
      <CustomerFormDialog customer={customer} onClose={onClose} />,
      { authSession: session },
    )

    await user.clear(screen.getByLabelText('Email (optional)'))
    await user.clear(screen.getByLabelText('Phone (optional)'))
    await user.clear(screen.getByLabelText('Notes (optional)'))
    await user.click(screen.getByRole('button', { name: 'Save' }))

    await waitFor(() =>
      expect(put).toHaveBeenCalledWith('/api/customers/5', {
        firstName: 'Alice',
        lastName: 'Murphy',
        email: null,
        phone: null,
        notes: null,
      }),
    )
    expect(onClose).toHaveBeenCalledOnce()
  })

  it('displays save errors without closing the dialog', async () => {
    mockHttp('post').mockRejectedValueOnce(
      apiError('Unable to save customer.', { status: 400 }),
    )
    const onClose = vi.fn()
    const { user } = renderWithProviders(
      <CustomerFormDialog customer={null} onClose={onClose} />,
      { authSession: session },
    )

    await user.type(screen.getByLabelText(/^First name/), 'Ava')
    await user.type(screen.getByLabelText(/^Last name/), 'Walsh')
    await user.click(screen.getByRole('button', { name: 'Save' }))

    expect(await screen.findByText('Unable to save customer.')).toBeInTheDocument()
    expect(onClose).not.toHaveBeenCalled()
  })
})
