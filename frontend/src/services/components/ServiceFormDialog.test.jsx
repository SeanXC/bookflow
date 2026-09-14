import { describe, expect, it, vi } from 'vitest'

import { apiError, apiResponse, mockHttp } from '../../test/httpMock.js'
import {
  renderWithProviders,
  screen,
  waitFor,
} from '../../test/testUtils.jsx'
import ServiceFormDialog from './ServiceFormDialog.jsx'

const ownerSession = {
  accessToken: 'owner-token',
  user: { email: 'owner@example.com', role: 'OWNER' },
}

const service = {
  id: 22,
  name: 'Consultation',
  description: 'Personal consultation',
  price: 45,
  durationMinutes: 60,
  active: true,
}

describe('ServiceFormDialog', () => {
  it('creates a normalized service', async () => {
    const post = mockHttp('post').mockResolvedValueOnce(
      apiResponse({ ...service, id: 23 }),
    )
    const onClose = vi.fn()
    const { user } = renderWithProviders(
      <ServiceFormDialog onClose={onClose} service={null} />,
      { authSession: ownerSession },
    )

    await user.type(screen.getByLabelText(/^Service name/), '  Haircut  ')
    await user.type(
      screen.getByLabelText('Description (optional)'),
      '  Tailored haircut  ',
    )
    await user.type(screen.getByLabelText(/^Price/), '75.50')
    await user.type(screen.getByLabelText(/^Duration \(minutes\)/), '45')
    await user.click(screen.getByRole('button', { name: 'Save' }))

    await waitFor(() =>
      expect(post).toHaveBeenCalledWith('/api/services', {
        name: 'Haircut',
        description: 'Tailored haircut',
        price: 75.5,
        durationMinutes: 45,
      }),
    )
    expect(onClose).toHaveBeenCalledOnce()
  })

  it('updates an existing service and normalizes an empty description', async () => {
    const put = mockHttp('put').mockResolvedValueOnce(apiResponse(service))
    const onClose = vi.fn()
    const { user } = renderWithProviders(
      <ServiceFormDialog onClose={onClose} service={service} />,
      { authSession: ownerSession },
    )

    expect(screen.getByText('Edit service')).toBeInTheDocument()
    await user.clear(screen.getByLabelText(/^Service name/))
    await user.type(screen.getByLabelText(/^Service name/), 'Updated service')
    await user.clear(screen.getByLabelText('Description (optional)'))
    await user.clear(screen.getByLabelText(/^Price/))
    await user.type(screen.getByLabelText(/^Price/), '50')
    await user.clear(screen.getByLabelText(/^Duration \(minutes\)/))
    await user.type(screen.getByLabelText(/^Duration \(minutes\)/), '30')
    await user.click(screen.getByRole('button', { name: 'Save' }))

    await waitFor(() =>
      expect(put).toHaveBeenCalledWith('/api/services/22', {
        name: 'Updated service',
        description: null,
        price: 50,
        durationMinutes: 30,
      }),
    )
    expect(onClose).toHaveBeenCalledOnce()
  })

  it('keeps the dialog open and displays API errors', async () => {
    mockHttp('post').mockRejectedValueOnce(
      apiError('Unable to create service.', { status: 400 }),
    )
    const onClose = vi.fn()
    const { user } = renderWithProviders(
      <ServiceFormDialog onClose={onClose} service={null} />,
      { authSession: ownerSession },
    )

    await user.type(screen.getByLabelText(/^Service name/), 'Haircut')
    await user.type(screen.getByLabelText(/^Price/), '75')
    await user.type(screen.getByLabelText(/^Duration \(minutes\)/), '45')
    await user.click(screen.getByRole('button', { name: 'Save' }))

    expect(
      await screen.findByText('Unable to create service.'),
    ).toBeInTheDocument()
    expect(onClose).not.toHaveBeenCalled()
  })
})
