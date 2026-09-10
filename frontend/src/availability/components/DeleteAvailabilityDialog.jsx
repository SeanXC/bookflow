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

function DeleteAvailabilityDialog({
  description,
  errorMessage,
  isPending,
  onClose,
  onConfirm,
  title,
}) {
  return (
    <Dialog onClose={isPending ? undefined : onClose} open>
      <DialogTitle>{title}</DialogTitle>
      <DialogContent>
        <DialogContentText>{description}</DialogContentText>
        {errorMessage && (
          <Alert severity="error" sx={{ mt: 2 }}>
            {errorMessage}
          </Alert>
        )}
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 3 }}>
        <Button disabled={isPending} onClick={onClose}>
          Cancel
        </Button>
        <Button
          color="error"
          disabled={isPending}
          onClick={onConfirm}
          variant="contained"
        >
          {isPending ? 'Deleting…' : 'Delete'}
        </Button>
      </DialogActions>
    </Dialog>
  )
}

DeleteAvailabilityDialog.propTypes = {
  description: PropTypes.string.isRequired,
  errorMessage: PropTypes.string,
  isPending: PropTypes.bool.isRequired,
  onClose: PropTypes.func.isRequired,
  onConfirm: PropTypes.func.isRequired,
  title: PropTypes.string.isRequired,
}

export default DeleteAvailabilityDialog
