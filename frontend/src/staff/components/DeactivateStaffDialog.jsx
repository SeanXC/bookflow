import { useEffect, useState } from 'react'
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

import { useDeactivateStaff } from '../api/staffMutations.js'

function DeactivateStaffDialog({ onClose, staff }) {
  const deactivateMutation = useDeactivateStaff()
  const [errorMessage, setErrorMessage] = useState('')
  const open = staff !== null

  useEffect(() => {
    setErrorMessage('')
  }, [staff])

  async function handleDeactivate() {
    if (!staff) {
      return
    }

    setErrorMessage('')
    try {
      await deactivateMutation.mutateAsync(staff.id)
      onClose()
    } catch (error) {
      setErrorMessage(
        error instanceof Error ? error.message : 'Unable to deactivate staff.',
      )
    }
  }

  return (
    <Dialog
      onClose={() => {
        if (!deactivateMutation.isPending) {
          onClose()
        }
      }}
      open={open}
    >
      <DialogTitle>Deactivate staff member?</DialogTitle>
      <DialogContent>
        <DialogContentText>
          {staff
            ? `${staff.firstName} ${staff.lastName} will no longer be available for new appointments.`
            : ''}
        </DialogContentText>
        {errorMessage && (
          <Alert severity="error" sx={{ mt: 2 }}>
            {errorMessage}
          </Alert>
        )}
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 3 }}>
        <Button disabled={deactivateMutation.isPending} onClick={onClose}>
          Cancel
        </Button>
        <Button
          color="error"
          disabled={deactivateMutation.isPending}
          onClick={handleDeactivate}
          variant="contained"
        >
          {deactivateMutation.isPending ? 'Deactivating…' : 'Deactivate'}
        </Button>
      </DialogActions>
    </Dialog>
  )
}

DeactivateStaffDialog.propTypes = {
  onClose: PropTypes.func.isRequired,
  staff: PropTypes.shape({
    id: PropTypes.number.isRequired,
    firstName: PropTypes.string.isRequired,
    lastName: PropTypes.string.isRequired,
  }),
}

export default DeactivateStaffDialog
