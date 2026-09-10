import { Route, Routes } from 'react-router-dom'
import { describe, expect, it } from 'vitest'

import { apiResponse, mockHttp } from '../../test/httpMock.js'
import {
  fireEvent,
  renderWithProviders,
  screen,
  waitFor,
  within,
} from '../../test/testUtils.jsx'
import { formatLocalDate } from '../format.js'
import { MAX_EXCEPTION_RANGE_DAYS } from '../constants.js'
import StaffAvailabilityPage from './StaffAvailabilityPage.jsx'

function defaultRange() {
  const fromDate = new Date()
  const toDate = new Date()
  toDate.setDate(toDate.getDate() + (MAX_EXCEPTION_RANGE_DAYS - 1))
  return {
    from: formatLocalDate(fromDate),
    to: formatLocalDate(toDate),
  }
}

function renderPage() {
  return renderWithProviders(
    <Routes>
      <Route
        element={<StaffAvailabilityPage />}
        path="/staff/:staffId/availability"
      />
    </Routes>,
    {
      route: '/staff/40/availability',
      authSession: {
        accessToken: 'owner-token',
        user: { email: 'owner@example.com', role: 'OWNER' },
      },
    },
  )
}

describe('StaffAvailabilityPage', () => {
  it('loads hours and exceptions then deletes a weekly window', async () => {
    const range = defaultRange()
    const get = mockHttp('get').mockImplementation((url) => {
      if (url === '/api/staff/40') {
        return Promise.resolve(
          apiResponse({
            id: 40,
            firstName: 'Anna',
            lastName: 'Smith',
            phone: null,
            userId: null,
            active: true,
          }),
        )
      }
      if (url === '/api/staff/40/availability/weekly-hours') {
        return Promise.resolve(
          apiResponse([
            {
              id: 7,
              staffId: 40,
              dayOfWeek: 'MONDAY',
              startTime: '09:00:00',
              endTime: '17:00:00',
            },
          ]),
        )
      }
      if (url === '/api/staff/40/availability/exceptions') {
        return Promise.resolve(
          apiResponse([
            {
              id: 9,
              staffId: 40,
              exceptionDate: '2026-09-14',
              type: 'UNAVAILABLE',
              startTime: null,
              endTime: null,
              note: 'Holiday',
            },
          ]),
        )
      }
      return Promise.reject(new Error(`Unexpected GET ${url}`))
    })
    const del = mockHttp('delete').mockResolvedValueOnce(apiResponse(null))
    const { user } = renderPage()

    expect(
      await screen.findByRole('heading', { name: 'Anna Smith' }),
    ).toBeInTheDocument()
    expect(await screen.findByText('Monday')).toBeInTheDocument()
    expect(await screen.findByText('Holiday')).toBeInTheDocument()

    await waitFor(() =>
      expect(get).toHaveBeenCalledWith(
        '/api/staff/40/availability/exceptions',
        {
          params: range,
        },
      ),
    )

    await user.click(screen.getAllByRole('button', { name: 'Delete' })[0])
    await user.click(
      within(screen.getByRole('dialog')).getByRole('button', {
        name: 'Delete',
      }),
    )

    await waitFor(() =>
      expect(del).toHaveBeenCalledWith(
        '/api/staff/40/availability/weekly-hours/7',
      ),
    )
  })

  it('does not fetch exceptions for an inverted date range', async () => {
    const get = mockHttp('get').mockImplementation((url) => {
      if (url === '/api/staff/40') {
        return Promise.resolve(
          apiResponse({
            id: 40,
            firstName: 'Anna',
            lastName: 'Smith',
            phone: null,
            userId: null,
            active: true,
          }),
        )
      }
      if (url === '/api/staff/40/availability/weekly-hours') {
        return Promise.resolve(apiResponse([]))
      }
      if (url === '/api/staff/40/availability/exceptions') {
        return Promise.resolve(apiResponse([]))
      }
      return Promise.reject(new Error(`Unexpected GET ${url}`))
    })
    renderPage()

    await waitFor(() =>
      expect(
        get.mock.calls.some(
          ([url]) => url === '/api/staff/40/availability/exceptions',
        ),
      ).toBe(true),
    )
    const exceptionCallsBefore = get.mock.calls.filter(
      ([url]) => url === '/api/staff/40/availability/exceptions',
    ).length

    fireEvent.change(await screen.findByLabelText(/^To/), {
      target: { value: '2020-01-01' },
    })

    expect(
      await screen.findByText(
        'The start date must not be after the end date.',
      ),
    ).toBeInTheDocument()
    expect(
      get.mock.calls.filter(
        ([url]) => url === '/api/staff/40/availability/exceptions',
      ).length,
    ).toBe(exceptionCallsBefore)
  })
})
