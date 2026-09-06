import { useMemo, useState } from 'react'
import {
  Alert,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControl,
  InputLabel,
  MenuItem,
  Select,
  Stack,
  TextField,
} from '@mui/material'
import PropTypes from 'prop-types'

import { useCreateStaff, useUpdateStaff } from '../api/staffMutations.js'

function StaffFormDialog({ onClose, open, staff, users }) {
  const createMutation = useCreateStaff()
  const updateMutation = useUpdateStaff()
  const [firstName, setFirstName] = useState(staff?.firstName ?? '')
  const [lastName, setLastName] = useState(staff?.lastName ?? '')
  const [phone, setPhone] = useState(staff?.phone ?? '')
  const [userId, setUserId] = useState(
    /** @type {number | ''} */ (staff?.userId ?? ''),
  )
  const [errorMessage, setErrorMessage] = useState('')
  const isEditing = staff !== null
  const isSubmitting = createMutation.isPending || updateMutation.isPending

  const staffUsers = useMemo(
    () =>
      users.filter(
        (user) =>
          user.role === 'STAFF' &&
          (user.enabled || user.id === staff?.userId),
      ),
    [staff?.userId, users],
  )

  /**
   * @param {import('react').FormEvent<HTMLFormElement>} event
   */
  async function handleSubmit(event) {
    event.preventDefault()
    setErrorMessage('')
    const request = {
      userId: userId === '' ? null : Number(userId),
      firstName: firstName.trim(),
      lastName: lastName.trim(),
      phone: phone.trim() || null,
    }

    try {
      if (staff) {
        await updateMutation.mutateAsync({ staffId: staff.id, request })
      } else {
        await createMutation.mutateAsync(request)
      }
      onClose()
    } catch (error) {
      setErrorMessage(
        error instanceof Error ? error.message : 'Unable to save staff.',
      )
    }
  }

  return (
    <Dialog
      fullWidth
      maxWidth="sm"
      onClose={() => {
        if (!isSubmitting) {
          onClose()
        }
      }}
      open={open}
    >
      <Stack component="form" onSubmit={handleSubmit}>
        <DialogTitle>{isEditing ? 'Edit staff' : 'Create staff'}</DialogTitle>
        <DialogContent>
          <Stack spacing={2.5} sx={{ pt: 1 }}>
            {errorMessage && <Alert severity="error">{errorMessage}</Alert>}
            <TextField
              autoFocus
              fullWidth
              label="First name"
              onChange={(event) => setFirstName(event.target.value)}
              required
              slotProps={{ htmlInput: { maxLength: 100 } }}
              value={firstName}
            />
            <TextField
              fullWidth
              label="Last name"
              onChange={(event) => setLastName(event.target.value)}
              required
              slotProps={{ htmlInput: { maxLength: 100 } }}
              value={lastName}
            />
            <TextField
              fullWidth
              label="Phone (optional)"
              onChange={(event) => setPhone(event.target.value)}
              slotProps={{ htmlInput: { maxLength: 30 } }}
              type="tel"
              value={phone}
            />
            <FormControl fullWidth>
              <InputLabel id="staff-user-label">Login account</InputLabel>
              <Select
                label="Login account"
                labelId="staff-user-label"
                onChange={(event) => setUserId(event.target.value)}
                value={userId}
              >
                <MenuItem value="">No linked account</MenuItem>
                {staffUsers.map((user) => (
                  <MenuItem key={user.id} value={user.id}>
                    {user.email}
                    {!user.enabled ? ' (disabled)' : ''}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
          </Stack>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 3 }}>
          <Button disabled={isSubmitting} onClick={onClose}>
            Cancel
          </Button>
          <Button disabled={isSubmitting} type="submit" variant="contained">
            {isSubmitting ? 'Saving…' : 'Save'}
          </Button>
        </DialogActions>
      </Stack>
    </Dialog>
  )
}

StaffFormDialog.propTypes = {
  onClose: PropTypes.func.isRequired,
  open: PropTypes.bool.isRequired,
  staff: PropTypes.shape({
    id: PropTypes.number.isRequired,
    userId: PropTypes.number,
    firstName: PropTypes.string.isRequired,
    lastName: PropTypes.string.isRequired,
    phone: PropTypes.string,
    active: PropTypes.bool.isRequired,
  }),
  users: PropTypes.arrayOf(
    PropTypes.shape({
      id: PropTypes.number.isRequired,
      email: PropTypes.string.isRequired,
      role: PropTypes.string.isRequired,
      enabled: PropTypes.bool.isRequired,
    }).isRequired,
  ).isRequired,
}

export default StaffFormDialog
