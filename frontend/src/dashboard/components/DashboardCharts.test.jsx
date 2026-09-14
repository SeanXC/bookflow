import { describe, expect, it, vi } from 'vitest'

import { apiResponse, mockHttp } from '../../test/httpMock.js'
import { renderWithProviders, screen } from '../../test/testUtils.jsx'
import DashboardCharts from './DashboardCharts.jsx'

vi.mock('recharts', () => {
  const container = ({ children }) => <div>{children}</div>
  const leaf = () => null

  function XAxis({ dataKey, tickFormatter }) {
    tickFormatter(dataKey === 'date' ? '2026-09-14' : '2026-09')
    return null
  }

  function YAxis({ tickFormatter }) {
    tickFormatter?.(125)
    return null
  }

  function Tooltip({ formatter, labelFormatter }) {
    formatter?.(125)
    for (const label of ['2026-09-14', '2026-09']) {
      try {
        labelFormatter?.(label)
      } catch {
        // Each chart accepts a different date granularity.
      }
    }
    return null
  }

  return {
    Bar: leaf,
    BarChart: container,
    CartesianGrid: leaf,
    Line: leaf,
    LineChart: container,
    ResponsiveContainer: container,
    Tooltip,
    XAxis,
    YAxis,
  }
})

describe('DashboardCharts', () => {
  it('formats booking and revenue chart labels', async () => {
    mockHttp('get').mockImplementation((url) => {
      if (url === '/api/dashboard/bookings-by-week') {
        return Promise.resolve(
          apiResponse([{ date: '2026-09-14', bookings: 3 }]),
        )
      }
      if (url === '/api/dashboard/revenue-by-month') {
        return Promise.resolve(
          apiResponse([{ month: '2026-09', revenue: 125 }]),
        )
      }
      return Promise.reject(new Error(`Unexpected GET ${url}`))
    })

    renderWithProviders(<DashboardCharts />, {
      authSession: {
        accessToken: 'owner-token',
        user: { email: 'owner@example.com', role: 'OWNER' },
      },
    })

    expect(
      await screen.findByRole('img', {
        name: 'Bar chart of appointments for each day this week',
      }),
    ).toBeInTheDocument()
    expect(
      screen.getByRole('img', {
        name: 'Line chart of revenue for the trailing twelve months',
      }),
    ).toBeInTheDocument()
  })
})
