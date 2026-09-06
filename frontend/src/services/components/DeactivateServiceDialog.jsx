import { useState } from 'react'
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

import { useDeactivateService } from '../api/serviceMutations.js'

function DeactivateServiceDialog({ onClose, service }) {
  const deactivateMutation = useDeactivateService()
  const [errorMessage, setErrorMessage] = useState('')

  async function handleDeactivate() {
    setErrorMessage('')
    try {
      await deactivateMutation.mutateAsync(service.id)
      onClose()
    } catch (error) {
      setErrorMessage(
        error instanceof Error
          ? error.message
          : 'Unable to deactivate service.',
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
      open
    >
      <DialogTitle>Deactivate service?</DialogTitle>
      <DialogContent>
        <DialogContentText>
          {service.name} will no longer be available for new appointments.
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

DeactivateServiceDialog.propTypes = {
  onClose: PropTypes.func.isRequired,
  service: PropTypes.shape({
    id: PropTypes.number.isRequired,
    name: PropTypes.string.isRequired,
  }).isRequired,
}

export default DeactivateServiceDialog
