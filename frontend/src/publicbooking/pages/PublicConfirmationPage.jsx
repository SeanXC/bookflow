import {
  Box,
  Button,
  Chip,
  Divider,
  Paper,
  Stack,
  Typography,
} from '@mui/material'
import PropTypes from 'prop-types'
import { Link as RouterLink, useLocation, useParams } from 'react-router-dom'

import { usePublicBusiness } from '../api/publicBookingQueries.js'
import { formatSlotRange } from '../format.js'

const currencyFormatter = new Intl.NumberFormat('en-IE', {
  style: 'currency',
  currency: 'EUR',
})

/**
 * @param {unknown} value
 * @returns {value is import('../types.js').PublicAppointment}
 */
function isPublicAppointment(value) {
  return (
    typeof value === 'object' &&
    value !== null &&
    'id' in value &&
    typeof value.id === 'number' &&
    'startTime' in value &&
    typeof value.startTime === 'string' &&
    'endTime' in value &&
    typeof value.endTime === 'string' &&
    'staff' in value &&
    typeof value.staff === 'object' &&
    value.staff !== null &&
    'service' in value &&
    typeof value.service === 'object' &&
    value.service !== null
  )
}

function PublicConfirmationPage() {
  const { slug } = useParams()
  const location = useLocation()
  const businessQuery = usePublicBusiness(slug)
  const appointment = isPublicAppointment(location.state?.appointment)
    ? location.state.appointment
    : null

  if (!appointment) {
    return (
      <Stack spacing={2.5}>
        <Typography component="h1" fontWeight={800} variant="h4">
          Confirmation unavailable
        </Typography>
        <Typography color="text.secondary">
          Open this page after completing a booking.
        </Typography>
        <Button
          component={RouterLink}
          sx={{ alignSelf: 'flex-start' }}
          to={`/book/${slug}`}
          variant="contained"
        >
          Back to booking page
        </Button>
      </Stack>
    )
  }

  const businessName = businessQuery.data?.name

  return (
    <Stack spacing={4}>
      <Stack spacing={1.25}>
        <Chip
          color="success"
          label={appointment.status}
          sx={{ alignSelf: 'flex-start' }}
        />
        <Typography component="h1" fontWeight={800} variant="h4">
          Appointment confirmed
        </Typography>
        <Typography color="text.secondary">
          {businessName
            ? `You're booked at ${businessName}.`
            : 'Your appointment is confirmed.'}
        </Typography>
      </Stack>

      <Paper
        elevation={0}
        sx={{ border: 1, borderColor: 'divider', borderRadius: 3, p: 3 }}
      >
        <Stack divider={<Divider flexItem />} spacing={2.5}>
          <ConfirmationDetail label="When">
            {formatSlotRange(appointment.startTime, appointment.endTime)}
          </ConfirmationDetail>
          <ConfirmationDetail label="Service">
            {appointment.service.name} ·{' '}
            {currencyFormatter.format(Number(appointment.service.price))} ·{' '}
            {appointment.service.durationMinutes} min
          </ConfirmationDetail>
          <ConfirmationDetail label="Staff">
            {appointment.staff.firstName} {appointment.staff.lastName}
          </ConfirmationDetail>
          <ConfirmationDetail label="Name">
            {appointment.customerFirstName} {appointment.customerLastName}
          </ConfirmationDetail>
          {appointment.notes ? (
            <ConfirmationDetail label="Notes">
              {appointment.notes}
            </ConfirmationDetail>
          ) : null}
        </Stack>
      </Paper>

      <Button
        component={RouterLink}
        sx={{ alignSelf: 'flex-start' }}
        to={`/book/${slug}`}
        variant="contained"
      >
        Book another appointment
      </Button>
    </Stack>
  )
}

function ConfirmationDetail({ label, children }) {
  return (
    <Box>
      <Typography color="text.secondary" variant="body2">
        {label}
      </Typography>
      <Typography mt={0.5} sx={{ whiteSpace: 'pre-wrap' }}>
        {children}
      </Typography>
    </Box>
  )
}

ConfirmationDetail.propTypes = {
  children: PropTypes.node.isRequired,
  label: PropTypes.string.isRequired,
}

export default PublicConfirmationPage
