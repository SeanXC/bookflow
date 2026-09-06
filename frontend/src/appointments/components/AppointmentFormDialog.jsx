import { useMemo, useState } from 'react'
import {
  Alert,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControl,
  FormHelperText,
  InputLabel,
  MenuItem,
  Select,
  Stack,
  TextField,
  Typography,
} from '@mui/material'
import PropTypes from 'prop-types'

import { useCustomers } from '../../customers/api/customerQueries.js'
import { useServices } from '../../services/api/serviceQueries.js'
import { useStaff } from '../../staff/api/staffQueries.js'
import {
  useCreateAppointment,
  useUpdateAppointment,
} from '../api/appointmentMutations.js'
import { getAppointmentErrorMessage } from '../appointmentError.js'

/**
 * @param {string} instant
 * @returns {string}
 */
function toLocalInputValue(instant) {
  const date = new Date(instant)
  const localTime = date.getTime() - date.getTimezoneOffset() * 60_000
  return new Date(localTime).toISOString().slice(0, 16)
}

/**
 * @param {Array<{ id: number }>} options
 * @param {{ id: number } | undefined} current
 */
function includeCurrentOption(options, current) {
  if (!current || options.some((option) => option.id === current.id)) {
    return options
  }
  return [current, ...options]
}

function AppointmentFormDialog({ appointment, onClose, open }) {
  const [customerId, setCustomerId] = useState(appointment?.customerId ?? '')
  const [staffId, setStaffId] = useState(appointment?.staffId ?? '')
  const [serviceId, setServiceId] = useState(appointment?.serviceId ?? '')
  const [startTime, setStartTime] = useState(
    appointment ? toLocalInputValue(appointment.startTime) : '',
  )
  const [notes, setNotes] = useState(appointment?.notes ?? '')
  const [submitted, setSubmitted] = useState(false)
  const createMutation = useCreateAppointment()
  const updateMutation = useUpdateAppointment()
  const mutation = appointment ? updateMutation : createMutation

  const customersQuery = useCustomers({
    page: 0,
    pageSize: 100,
    sortModel: [{ field: 'lastName', sort: 'asc' }],
  })
  const staffQuery = useStaff({
    active: true,
    page: 0,
    pageSize: 100,
    sortModel: [{ field: 'lastName', sort: 'asc' }],
  })
  const servicesQuery = useServices({
    active: true,
    page: 0,
    pageSize: 100,
    sortModel: [{ field: 'name', sort: 'asc' }],
  })

  const customers = useMemo(
    () =>
      includeCurrentOption(
        customersQuery.data?.content ?? [],
        appointment?.customer,
      ),
    [appointment?.customer, customersQuery.data?.content],
  )
  const staff = useMemo(
    () =>
      includeCurrentOption(staffQuery.data?.content ?? [], appointment?.staff),
    [appointment?.staff, staffQuery.data?.content],
  )
  const services = useMemo(
    () =>
      includeCurrentOption(
        servicesQuery.data?.content ?? [],
        appointment?.service,
      ),
    [appointment?.service, servicesQuery.data?.content],
  )
  const selectedService = services.find(
    (service) => service.id === Number(serviceId),
  )
  const hasRequiredFields =
    Number(customerId) > 0 &&
    Number(staffId) > 0 &&
    Number(serviceId) > 0 &&
    startTime !== ''
  const hasOptionsError =
    customersQuery.isError || staffQuery.isError || servicesQuery.isError

  let estimatedEndTime = ''
  if (startTime && selectedService) {
    const endTime = new Date(startTime)
    endTime.setMinutes(endTime.getMinutes() + selectedService.durationMinutes)
    estimatedEndTime = endTime.toLocaleString('en-IE', {
      dateStyle: 'medium',
      timeStyle: 'short',
    })
  }

  async function handleSubmit(event) {
    event.preventDefault()
    setSubmitted(true)
    mutation.reset()

    if (!hasRequiredFields) {
      return
    }

    const request = {
      customerId: Number(customerId),
      staffId: Number(staffId),
      serviceId: Number(serviceId),
      startTime: new Date(startTime).toISOString(),
      notes: notes.trim() || null,
    }

    try {
      if (appointment) {
        await updateMutation.mutateAsync({
          appointmentId: appointment.id,
          request,
        })
      } else {
        await createMutation.mutateAsync(request)
      }
      onClose()
    } catch {
      // The mutation exposes the normalized API error below.
    }
  }

  return (
    <Dialog
      fullWidth
      maxWidth="sm"
      onClose={mutation.isPending ? undefined : onClose}
      open={open}
    >
      <form onSubmit={handleSubmit}>
        <DialogTitle>
          {appointment ? 'Edit appointment' : 'Create appointment'}
        </DialogTitle>
        <DialogContent>
          <Stack mt={1} spacing={2.5}>
            {hasOptionsError && (
              <Alert severity="error">
                Booking options could not be loaded. Close the form and try
                again.
              </Alert>
            )}
            {mutation.isError && (
              <Alert severity="error">
                {getAppointmentErrorMessage(mutation.error)}
              </Alert>
            )}

            <FormControl error={submitted && !customerId} fullWidth>
              <InputLabel id="appointment-customer-label">Customer</InputLabel>
              <Select
                label="Customer"
                labelId="appointment-customer-label"
                onChange={(event) => setCustomerId(event.target.value)}
                value={customerId}
              >
                {customers.map((customer) => (
                  <MenuItem key={customer.id} value={customer.id}>
                    {customer.firstName} {customer.lastName}
                  </MenuItem>
                ))}
              </Select>
              {submitted && !customerId && (
                <FormHelperText>Choose a customer.</FormHelperText>
              )}
            </FormControl>

            <FormControl error={submitted && !serviceId} fullWidth>
              <InputLabel id="appointment-service-label">Service</InputLabel>
              <Select
                label="Service"
                labelId="appointment-service-label"
                onChange={(event) => setServiceId(event.target.value)}
                value={serviceId}
              >
                {services.map((service) => (
                  <MenuItem key={service.id} value={service.id}>
                    {service.name} ({service.durationMinutes} min)
                  </MenuItem>
                ))}
              </Select>
              {submitted && !serviceId && (
                <FormHelperText>Choose a service.</FormHelperText>
              )}
            </FormControl>

            <FormControl error={submitted && !staffId} fullWidth>
              <InputLabel id="appointment-form-staff-label">Staff</InputLabel>
              <Select
                label="Staff"
                labelId="appointment-form-staff-label"
                onChange={(event) => setStaffId(event.target.value)}
                value={staffId}
              >
                {staff.map((staffMember) => (
                  <MenuItem key={staffMember.id} value={staffMember.id}>
                    {staffMember.firstName} {staffMember.lastName}
                  </MenuItem>
                ))}
              </Select>
              {submitted && !staffId && (
                <FormHelperText>Choose a staff member.</FormHelperText>
              )}
            </FormControl>

            <TextField
              error={submitted && !startTime}
              helperText={
                submitted && !startTime
                  ? 'Choose a start date and time.'
                  : 'Entered in your local time and sent to the server as UTC.'
              }
              label="Start date and time"
              onChange={(event) => setStartTime(event.target.value)}
              required
              slotProps={{ inputLabel: { shrink: true } }}
              type="datetime-local"
              value={startTime}
            />

            {selectedService && (
              <Typography color="text.secondary" variant="body2">
                Duration: {selectedService.durationMinutes} minutes
                {estimatedEndTime ? ` · Estimated end: ${estimatedEndTime}` : ''}
              </Typography>
            )}

            <TextField
              label="Notes"
              minRows={3}
              multiline
              onChange={(event) => setNotes(event.target.value)}
              value={notes}
            />
          </Stack>
        </DialogContent>
        <DialogActions>
          <Button disabled={mutation.isPending} onClick={onClose}>
            Cancel
          </Button>
          <Button
            disabled={mutation.isPending || hasOptionsError}
            type="submit"
            variant="contained"
          >
            {mutation.isPending
              ? 'Saving...'
              : appointment
                ? 'Save changes'
                : 'Create appointment'}
          </Button>
        </DialogActions>
      </form>
    </Dialog>
  )
}

AppointmentFormDialog.propTypes = {
  appointment: PropTypes.object,
  onClose: PropTypes.func.isRequired,
  open: PropTypes.bool.isRequired,
}

export default AppointmentFormDialog
