import ArrowBackIcon from '@mui/icons-material/ArrowBack'
import {
  Alert,
  Box,
  Button,
  CircularProgress,
  Paper,
  Stack,
  Typography,
} from '@mui/material'
import { Link as RouterLink, useParams } from 'react-router-dom'

import { ApiError } from '../../api/apiError.js'
import {
  usePublicBusiness,
  usePublicServices,
  usePublicStaff,
} from '../api/publicBookingQueries.js'
import PublicUnavailableState from '../components/PublicUnavailableState.jsx'

const currencyFormatter = new Intl.NumberFormat('en-IE', {
  style: 'currency',
  currency: 'EUR',
})

function PublicServicePage() {
  const { slug, serviceId: serviceIdParam } = useParams()
  const serviceId = Number(serviceIdParam)
  const hasValidServiceId = Number.isInteger(serviceId) && serviceId > 0
  const businessQuery = usePublicBusiness(slug)
  const servicesQuery = usePublicServices(slug)
  const staffQuery = usePublicStaff(slug)

  if (!hasValidServiceId) {
    return <Alert severity="error">Invalid service.</Alert>
  }

  if (
    businessQuery.isPending ||
    servicesQuery.isPending ||
    staffQuery.isPending
  ) {
    return (
      <Box sx={{ display: 'grid', minHeight: 240, placeItems: 'center' }}>
        <CircularProgress aria-label="Loading service" />
      </Box>
    )
  }

  if (
    (businessQuery.error instanceof ApiError &&
      businessQuery.error.status === 404) ||
    (servicesQuery.error instanceof ApiError &&
      servicesQuery.error.status === 404) ||
    (staffQuery.error instanceof ApiError && staffQuery.error.status === 404)
  ) {
    return (
      <PublicUnavailableState
        onRetry={() => {
          businessQuery.refetch()
          servicesQuery.refetch()
          staffQuery.refetch()
        }}
      />
    )
  }

  if (businessQuery.isError || servicesQuery.isError || staffQuery.isError) {
    const message =
      businessQuery.error?.message ??
      servicesQuery.error?.message ??
      staffQuery.error?.message ??
      'Unable to load this service.'
    return (
      <Alert
        action={
          <Button
            color="inherit"
            onClick={() => {
              businessQuery.refetch()
              servicesQuery.refetch()
              staffQuery.refetch()
            }}
          >
            Retry
          </Button>
        }
        severity="error"
      >
        {message}
      </Alert>
    )
  }

  const service = (servicesQuery.data ?? []).find(
    (item) => item.id === serviceId,
  )
  if (!service) {
    return (
      <Stack spacing={2}>
        <Button
          component={RouterLink}
          startIcon={<ArrowBackIcon />}
          to={`/book/${slug}`}
        >
          Back to services
        </Button>
        <Alert severity="error">This service is not available to book.</Alert>
      </Stack>
    )
  }

  const staff = staffQuery.data ?? []

  return (
    <Stack spacing={4}>
      <Box>
        <Button
          component={RouterLink}
          startIcon={<ArrowBackIcon />}
          to={`/book/${slug}`}
        >
          Back to {businessQuery.data.name}
        </Button>
      </Box>

      <Stack spacing={1.25}>
        <Typography component="h1" fontWeight={800} variant="h4">
          {service.name}
        </Typography>
        {service.description ? (
          <Typography color="text.secondary">{service.description}</Typography>
        ) : null}
        <Typography color="text.secondary">
          {currencyFormatter.format(Number(service.price))} ·{' '}
          {service.durationMinutes} min
        </Typography>
      </Stack>

      <Stack spacing={2}>
        <Typography component="h2" fontWeight={800} variant="h5">
          Staff
        </Typography>
        {staff.length === 0 ? (
          <Typography color="text.secondary">
            No staff members are available to book right now.
          </Typography>
        ) : (
          <Box
            sx={{
              display: 'grid',
              gap: 2,
              gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr' },
            }}
          >
            {staff.map((member) => (
              <Paper
                elevation={0}
                key={member.id}
                sx={{
                  border: 1,
                  borderColor: 'divider',
                  borderRadius: 3,
                  p: 3,
                }}
              >
                <Typography fontWeight={800} variant="h6">
                  {member.firstName} {member.lastName}
                </Typography>
                <Typography color="text.secondary" mt={1}>
                  Available for this service
                </Typography>
              </Paper>
            ))}
          </Box>
        )}
      </Stack>
    </Stack>
  )
}

export default PublicServicePage
