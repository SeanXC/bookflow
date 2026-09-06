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

import { useUpdateUserEnabled } from '../api/userMutations.js'

function UserEnabledDialog({ onClose, user }) {
  const mutation = useUpdateUserEnabled()
  const nextEnabled = !user.enabled
  const action = nextEnabled ? 'Enable' : 'Disable'

  async function handleConfirm() {
    try {
      await mutation.mutateAsync({
        userId: user.id,
        enabled: nextEnabled,
      })
      onClose()
    } catch {
      // The normalized API error is displayed below.
    }
  }

  return (
    <Dialog
      fullWidth
      maxWidth="xs"
      onClose={mutation.isPending ? undefined : onClose}
      open
    >
      <DialogTitle>{action} login account</DialogTitle>
      <DialogContent>
        <DialogContentText>
          {nextEnabled
            ? 'Allow this user to sign in again?'
            : 'Prevent this user from signing in and using the API?'}
          <br />
          <strong>{user.email}</strong>
        </DialogContentText>
        {mutation.isError && (
          <Alert severity="error" sx={{ mt: 2 }}>
            {mutation.error.message}
          </Alert>
        )}
      </DialogContent>
      <DialogActions>
        <Button disabled={mutation.isPending} onClick={onClose}>
          Cancel
        </Button>
        <Button
          color={nextEnabled ? 'primary' : 'error'}
          disabled={mutation.isPending}
          onClick={handleConfirm}
          variant="contained"
        >
          {mutation.isPending ? 'Saving…' : action}
        </Button>
      </DialogActions>
    </Dialog>
  )
}

UserEnabledDialog.propTypes = {
  onClose: PropTypes.func.isRequired,
  user: PropTypes.shape({
    id: PropTypes.number.isRequired,
    email: PropTypes.string.isRequired,
    enabled: PropTypes.bool.isRequired,
  }).isRequired,
}

export default UserEnabledDialog
