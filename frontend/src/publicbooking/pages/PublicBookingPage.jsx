import { useState } from 'react'
import ArrowBackIcon from '@mui/icons-material/ArrowBack'
import {
  Alert,
  Box,
  Button,
  CircularProgress,
  Stack,
  Typography,
} from '@mui/material'
import { Link as RouterLink, useNavigate, useParams } from 'react-router-dom'

import { ApiError } from '../../api/apiError.js'
import { useCreatePublicAppointment } from '../api/publicBookingMutations.js'
import {
  usePublicBusiness,
  usePublicServices,
  usePublicSlots,
  usePublicStaff,
} from '../api/publicBookingQueries.js'
import PublicBookingForm from '../components/PublicBookingForm.jsx'
import PublicSlotPicker from '../components/PublicSlotPicker.jsx'
import PublicUnavailableState from '../components/PublicUnavailableState.jsx'
import { defaultSlotRange, getSlotRangeError } from '../format.js'
import { getPublicBookingErrorMessage } from '../publicBookingError.js'

const currencyFormatter = new Intl.NumberFormat('en-IE', {
  style: 'currency',
  currency: 'EUR',
})

function PublicBookingPage() {
  const { slug, serviceId: serviceIdParam, staffId: staffIdParam } = useParams()
  const navigate = useNavigate()
  const serviceId = Number(serviceIdParam)
  const staffId = Number(staffIdParam)
  const hasValidIds =
    Number.isInteger(serviceId) &&
    serviceId > 0 &&
    Number.isInteger(staffId) &&
    staffId > 0
  const [slotRange, setSlotRange] = useState(defaultSlotRange)
  const [selectedSlot, setSelectedSlot] = useState(
    /** @type {import('../types.js').PublicAvailableSlot | null} */ (null),
  )
  const rangeError = getSlotRangeError(slotRange.from, slotRange.to)
  const businessQuery = usePublicBusiness(slug)
  const servicesQuery = usePublicServices(slug)
  const staffQuery = usePublicStaff(slug)
  const slotsQuery = usePublicSlots(
    {
      slug: slug ?? '',
      staffId,
      serviceId,
      from: slotRange.from,
      to: slotRange.to,
    },
    hasValidIds && !rangeError,
  )
  const createMutation = useCreatePublicAppointment(slug ?? '')

  if (!hasValidIds) {
    return <Alert severity="error">Invalid booking selection.</Alert>
  }

  if (
    businessQuery.isPending ||
    servicesQuery.isPending ||
    staffQuery.isPending
  ) {
    return (
      <Box sx={{ display: 'grid', minHeight: 240, placeItems: 'center' }}>
        <CircularProgress aria-label="Loading booking form" />
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
      'Unable to load this booking page.'
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
  const staff = (staffQuery.data ?? []).find((item) => item.id === staffId)
  if (!service || !staff) {
    return (
      <Stack spacing={2}>
        <Button
          component={RouterLink}
          startIcon={<ArrowBackIcon />}
          to={`/book/${slug}/services/${serviceId}`}
        >
          Back to service
        </Button>
        <Alert severity="error">
          This staff member or service is not available to book.
        </Alert>
      </Stack>
    )
  }

  /**
   * @param {{
   *   firstName: string,
   *   lastName: string,
   *   email: string,
   *   phone: string,
   *   notes: string | null
   * }} guest
   */
  async function handleSubmit(guest) {
    if (!selectedSlot) {
      return
    }
    createMutation.reset()
    try {
      const appointment = await createMutation.mutateAsync({
        staffId,
        serviceId,
        startTime: selectedSlot.startTime,
        firstName: guest.firstName,
        lastName: guest.lastName,
        email: guest.email,
        phone: guest.phone,
        notes: guest.notes,
      })
      navigate(`/book/${slug}/confirmation`, {
        replace: true,
        state: { appointment },
      })
    } catch {
      // The mutation error is rendered by the form.
    }
  }

  return (
    <Stack spacing={4}>
      <Box>
        <Button
          component={RouterLink}
          startIcon={<ArrowBackIcon />}
          to={`/book/${slug}/services/${serviceId}`}
        >
          Back to {service.name}
        </Button>
      </Box>

      <Stack spacing={1.25}>
        <Typography component="h1" fontWeight={800} variant="h4">
          Book {service.name}
        </Typography>
        <Typography color="text.secondary">
          {staff.firstName} {staff.lastName} ·{' '}
          {currencyFormatter.format(Number(service.price))} ·{' '}
          {service.durationMinutes} min
        </Typography>
      </Stack>

      <PublicSlotPicker
        from={slotRange.from}
        onRangeChange={(nextRange) => {
          setSelectedSlot(null)
          setSlotRange(nextRange)
        }}
        onSelect={setSelectedSlot}
        rangeError={rangeError}
        selectedStartTime={selectedSlot?.startTime}
        slots={slotsQuery.data ?? []}
        slotsError={
          slotsQuery.isError
            ? (slotsQuery.error?.message ?? 'Unable to load available times.')
            : null
        }
        slotsLoading={slotsQuery.isPending}
        to={slotRange.to}
      />

      <PublicBookingForm
        errorMessage={
          createMutation.isError
            ? getPublicBookingErrorMessage(createMutation.error)
            : null
        }
        isSubmitting={createMutation.isPending}
        onSubmit={handleSubmit}
        selectedSlot={selectedSlot}
      />
    </Stack>
  )
}

export default PublicBookingPage
