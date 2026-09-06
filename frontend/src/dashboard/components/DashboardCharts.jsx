import { Alert, Box, Paper, Skeleton, Typography } from '@mui/material'
import {
  Bar,
  BarChart,
  CartesianGrid,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts'

import {
  useBookingsByWeek,
  useRevenueByMonth,
} from '../api/dashboardQueries.js'

const currencyFormatter = new Intl.NumberFormat('en-IE', {
  style: 'currency',
  currency: 'EUR',
  maximumFractionDigits: 0,
})

const dayFormatter = new Intl.DateTimeFormat('en-IE', {
  weekday: 'short',
})

const monthFormatter = new Intl.DateTimeFormat('en-IE', {
  month: 'short',
})

/** @param {string} date */
function formatDay(date) {
  return dayFormatter.format(new Date(`${date}T00:00:00`))
}

/** @param {string} month */
function formatMonth(month) {
  return monthFormatter.format(new Date(`${month}-01T00:00:00`))
}

function DashboardCharts() {
  const bookingsQuery = useBookingsByWeek()
  const revenueQuery = useRevenueByMonth()

  return (
    <Box
      sx={{
        display: 'grid',
        gap: 2,
        gridTemplateColumns: { xs: '1fr', lg: 'repeat(2, minmax(0, 1fr))' },
      }}
    >
      <Paper
        elevation={0}
        sx={{ border: 1, borderColor: 'divider', borderRadius: 3, p: 3 }}
      >
        <Typography component="h2" fontWeight={700} mb={3} variant="h6">
          Appointments this week
        </Typography>
        {bookingsQuery.isError ? (
          <Alert severity="error">{bookingsQuery.error.message}</Alert>
        ) : bookingsQuery.isPending ? (
          <Skeleton height={300} variant="rounded" />
        ) : (
          <Box
            aria-label="Bar chart of appointments for each day this week"
            role="img"
            sx={{ height: 300 }}
          >
            <ResponsiveContainer height="100%" width="100%">
              <BarChart data={bookingsQuery.data}>
                <CartesianGrid strokeDasharray="3 3" vertical={false} />
                <XAxis dataKey="date" tickFormatter={formatDay} />
                <YAxis allowDecimals={false} width={36} />
                <Tooltip
                  labelFormatter={(date) =>
                    new Date(`${date}T00:00:00`).toLocaleDateString('en-IE', {
                      dateStyle: 'full',
                    })
                  }
                />
                <Bar
                  dataKey="bookings"
                  fill="#1976d2"
                  name="Appointments"
                  radius={[6, 6, 0, 0]}
                />
              </BarChart>
            </ResponsiveContainer>
          </Box>
        )}
      </Paper>

      <Paper
        elevation={0}
        sx={{ border: 1, borderColor: 'divider', borderRadius: 3, p: 3 }}
      >
        <Typography component="h2" fontWeight={700} mb={3} variant="h6">
          Revenue by month
        </Typography>
        {revenueQuery.isError ? (
          <Alert severity="error">{revenueQuery.error.message}</Alert>
        ) : revenueQuery.isPending ? (
          <Skeleton height={300} variant="rounded" />
        ) : (
          <Box
            aria-label="Line chart of revenue for the trailing twelve months"
            role="img"
            sx={{ height: 300 }}
          >
            <ResponsiveContainer height="100%" width="100%">
              <LineChart data={revenueQuery.data}>
                <CartesianGrid strokeDasharray="3 3" vertical={false} />
                <XAxis dataKey="month" tickFormatter={formatMonth} />
                <YAxis tickFormatter={(value) => `€${value}`} width={65} />
                <Tooltip
                  formatter={(value) => [
                    currencyFormatter.format(Number(value)),
                    'Revenue',
                  ]}
                  labelFormatter={(month) =>
                    new Date(`${month}-01T00:00:00`).toLocaleDateString(
                      'en-IE',
                      {
                        month: 'long',
                        year: 'numeric',
                      },
                    )
                  }
                />
                <Line
                  dataKey="revenue"
                  dot={false}
                  name="Revenue"
                  stroke="#2e7d32"
                  strokeWidth={3}
                  type="monotone"
                />
              </LineChart>
            </ResponsiveContainer>
          </Box>
        )}
      </Paper>
    </Box>
  )
}

export default DashboardCharts
