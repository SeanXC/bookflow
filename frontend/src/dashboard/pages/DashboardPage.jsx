import CalendarMonthOutlinedIcon from '@mui/icons-material/CalendarMonthOutlined'
import EventBusyOutlinedIcon from '@mui/icons-material/EventBusyOutlined'
import EuroOutlinedIcon from '@mui/icons-material/EuroOutlined'
import PeopleOutlineIcon from '@mui/icons-material/PeopleOutline'
import {
  Alert,
  Box,
  Button,
  Chip,
  Paper,
  Skeleton,
  Stack,
  Typography,
} from '@mui/material'
import PropTypes from 'prop-types'

import { useDashboardSummary } from '../api/dashboardQueries.js'
import DashboardCharts from '../components/DashboardCharts.jsx'
import RecentAppointments from '../components/RecentAppointments.jsx'

const currencyFormatter = new Intl.NumberFormat('en-IE', {
  style: 'currency',
  currency: 'EUR',
})

const numberFormatter = new Intl.NumberFormat('en-IE')

function MetricCard({ color, icon, label, loading, value }) {
  return (
    <Paper
      elevation={0}
      sx={{
        border: 1,
        borderColor: 'divider',
        borderRadius: 3,
        minHeight: 170,
        p: 3,
      }}
    >
      <Stack direction="row" justifyContent="space-between" spacing={2}>
        <Box>
          <Typography color="text.secondary" fontWeight={600} variant="body2">
            {label}
          </Typography>
          {loading ? (
            <Skeleton sx={{ mt: 1 }} width={120}>
              <Typography variant="h4">0</Typography>
            </Skeleton>
          ) : (
            <Typography fontWeight={800} mt={1} variant="h4">
              {value}
            </Typography>
          )}
        </Box>
        <Box
          aria-hidden="true"
          sx={{
            alignItems: 'center',
            bgcolor: 'action.hover',
            borderRadius: 2.5,
            color: `${color}.main`,
            display: 'flex',
            height: 52,
            justifyContent: 'center',
            width: 52,
          }}
        >
          {icon}
        </Box>
      </Stack>
    </Paper>
  )
}

MetricCard.propTypes = {
  color: PropTypes.oneOf(['primary', 'success', 'warning', 'error']).isRequired,
  icon: PropTypes.node.isRequired,
  label: PropTypes.string.isRequired,
  loading: PropTypes.bool.isRequired,
  value: PropTypes.string.isRequired,
}

function DashboardPage() {
  const summaryQuery = useDashboardSummary()
  const summary = summaryQuery.data
  const loading = summaryQuery.isPending

  return (
    <Stack spacing={3}>
      <Box
        sx={{
          alignItems: { sm: 'center' },
          display: 'flex',
          flexDirection: { xs: 'column', sm: 'row' },
          gap: 2,
          justifyContent: 'space-between',
        }}
      >
        <div>
          <Typography component="h1" fontWeight={800} variant="h4">
            Dashboard
          </Typography>
          <Typography color="text.secondary" mt={0.75}>
            Monitor your current business performance.
          </Typography>
        </div>
        {summary?.businessTimeZone && (
          <Chip
            label={`Business time zone: ${summary.businessTimeZone}`}
            variant="outlined"
          />
        )}
      </Box>

      {summaryQuery.isError && (
        <Alert
          action={
            <Button
              color="inherit"
              onClick={() => summaryQuery.refetch()}
              size="small"
            >
              Retry
            </Button>
          }
          severity="error"
        >
          {summaryQuery.error.message}
        </Alert>
      )}

      <Box
        aria-label="Dashboard summary"
        sx={{
          display: 'grid',
          gap: 2,
          gridTemplateColumns: {
            xs: '1fr',
            sm: 'repeat(2, minmax(0, 1fr))',
            xl: 'repeat(4, minmax(0, 1fr))',
          },
        }}
      >
        <MetricCard
          color="primary"
          icon={<CalendarMonthOutlinedIcon />}
          label="Today's appointments"
          loading={loading}
          value={numberFormatter.format(summary?.todayAppointments ?? 0)}
        />
        <MetricCard
          color="success"
          icon={<EuroOutlinedIcon />}
          label="Monthly revenue"
          loading={loading}
          value={currencyFormatter.format(summary?.monthlyRevenue ?? 0)}
        />
        <MetricCard
          color="warning"
          icon={<PeopleOutlineIcon />}
          label="Active customers"
          loading={loading}
          value={numberFormatter.format(summary?.activeCustomers ?? 0)}
        />
        <MetricCard
          color="error"
          icon={<EventBusyOutlinedIcon />}
          label="Cancellation rate"
          loading={loading}
          value={`${numberFormatter.format(summary?.cancellationRate ?? 0)}%`}
        />
      </Box>
      <DashboardCharts />
      <RecentAppointments />
    </Stack>
  )
}

export default DashboardPage
