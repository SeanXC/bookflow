import {
  Alert,
  Box,
  Button,
  CircularProgress,
  Link,
  Paper,
  Stack,
  Typography,
} from '@mui/material'
import { Link as RouterLink, useParams } from 'react-router-dom'

import { ApiError } from '../../api/apiError.js'
import {
  usePublicBusiness,
  usePublicServices,
} from '../api/publicBookingQueries.js'
import PublicUnavailableState from '../components/PublicUnavailableState.jsx'

const currencyFormatter = new Intl.NumberFormat('en-IE', {
  style: 'currency',
  currency: 'EUR',
})

function PublicBusinessPage() {
  const { slug } = useParams()
  const businessQuery = usePublicBusiness(slug)
  const servicesQuery = usePublicServices(slug)

  if (businessQuery.isPending || servicesQuery.isPending) {
    return (
      <Box sx={{ display: 'grid', minHeight: 240, placeItems: 'center' }}>
        <CircularProgress aria-label="Loading booking page" />
      </Box>
    )
  }

  if (
    (businessQuery.error instanceof ApiError &&
      businessQuery.error.status === 404) ||
    (servicesQuery.error instanceof ApiError &&
      servicesQuery.error.status === 404)
  ) {
    return (
      <PublicUnavailableState
        onRetry={() => {
          businessQuery.refetch()
          servicesQuery.refetch()
        }}
      />
    )
  }

  if (businessQuery.isError || servicesQuery.isError) {
    const message =
      businessQuery.error?.message ??
      servicesQuery.error?.message ??
      'Unable to load this booking page.'
    return (
      <Alert
        action={
          <Button
            color="inherit"
            onClick={() => {
              businessQuery.refetch()
              servicesQuery.refetch()
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

  const business = businessQuery.data
  const services = servicesQuery.data ?? []

  return (
    <Stack spacing={4}>
      <Stack spacing={1.25}>
        <Typography component="h1" fontWeight={800} variant="h4">
          {business.name}
        </Typography>
        {business.description ? (
          <Typography color="text.secondary">{business.description}</Typography>
        ) : null}
        {business.phone ? (
          <Typography>
            <Link href={`tel:${business.phone}`}>{business.phone}</Link>
          </Typography>
        ) : null}
      </Stack>

      <Paper
        component={RouterLink}
        elevation={0}
        sx={{
          border: 1,
          borderColor: 'divider',
          borderRadius: 3,
          color: 'inherit',
          display: 'block',
          p: 3,
          textDecoration: 'none',
          '&:hover': {
            borderColor: 'primary.main',
          },
        }}
        to={`/book/${slug}/assistant`}
      >
        <Typography fontWeight={800} variant="h6">
          Book with the assistant
        </Typography>
        <Typography color="text.secondary" mt={1}>
          Describe what you need. I will check real availability, then wait for
          you to confirm.
        </Typography>
      </Paper>

      <Stack spacing={2}>
        <Typography component="h2" fontWeight={800} variant="h5">
          Services
        </Typography>
        {services.length === 0 ? (
          <Typography color="text.secondary">
            No services are available to book right now.
          </Typography>
        ) : (
          <Box
            sx={{
              display: 'grid',
              gap: 2,
              gridTemplateColumns: { xs: '1fr', sm: '1fr 1fr' },
            }}
          >
            {services.map((service) => (
              <Paper
                component={RouterLink}
                elevation={0}
                key={service.id}
                sx={{
                  border: 1,
                  borderColor: 'divider',
                  borderRadius: 3,
                  color: 'inherit',
                  display: 'block',
                  p: 3,
                  textDecoration: 'none',
                  '&:hover': {
                    borderColor: 'primary.main',
                  },
                }}
                to={`/book/${slug}/services/${service.id}`}
              >
                <Typography fontWeight={800} variant="h6">
                  {service.name}
                </Typography>
                {service.description ? (
                  <Typography color="text.secondary" mt={1}>
                    {service.description}
                  </Typography>
                ) : null}
                <Typography color="text.secondary" mt={1.5}>
                  {currencyFormatter.format(Number(service.price))} ·{' '}
                  {service.durationMinutes} min
                </Typography>
              </Paper>
            ))}
          </Box>
        )}
      </Stack>
    </Stack>
  )
}

export default PublicBusinessPage
