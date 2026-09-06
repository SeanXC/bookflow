import {
  Alert,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
} from '@mui/material'
import PropTypes from 'prop-types'

import { useUpdateAppointmentStatus } from '../api/appointmentMutations.js'

const actionContent = {
  COMPLETED: {
    title: 'Complete appointment',
    message:
      'Mark this appointment as completed? This action cannot be reversed.',
    button: 'Complete',
    color: 'success',
  },
  CANCELLED: {
    title: 'Cancel appointment',
    message:
      'Cancel this appointment? The time slot will become available again.',
    button: 'Cancel appointment',
    color: 'error',
  },
}

function AppointmentStatusDialog({ appointment, onClose, status }) {
  const mutation = useUpdateAppointmentStatus()
  const content = actionContent[status]

  async function handleConfirm() {
    try {
      await mutation.mutateAsync({
        appointmentId: appointment.id,
        status,
      })
      onClose()
    } catch {
      // The mutation exposes the normalized API error below.
    }
  }

  return (
    <Dialog
      fullWidth
      maxWidth="xs"
      onClose={mutation.isPending ? undefined : onClose}
      open
    >
      <DialogTitle>{content.title}</DialogTitle>
      <DialogContent>
        <DialogContentText>
          {content.message}
          <br />
          <strong>
            {appointment.customer.firstName} {appointment.customer.lastName}
          </strong>
        </DialogContentText>
        {mutation.isError && (
          <Alert severity="error" sx={{ mt: 2 }}>
            {mutation.error.message}
          </Alert>
        )}
      </DialogContent>
      <DialogActions>
        <Button disabled={mutation.isPending} onClick={onClose}>
          Go back
        </Button>
        <Button
          color={content.color}
          disabled={mutation.isPending}
          onClick={handleConfirm}
          variant="contained"
        >
          {mutation.isPending ? 'Saving...' : content.button}
        </Button>
      </DialogActions>
    </Dialog>
  )
}

AppointmentStatusDialog.propTypes = {
  appointment: PropTypes.object.isRequired,
  onClose: PropTypes.func.isRequired,
  status: PropTypes.oneOf(['COMPLETED', 'CANCELLED']).isRequired,
}

export default AppointmentStatusDialog
