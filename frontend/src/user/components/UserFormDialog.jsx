import { useState } from 'react'
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

import { useCreateUser } from '../api/userMutations.js'

function UserFormDialog({ onClose }) {
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [role, setRole] = useState('STAFF')
  const mutation = useCreateUser()

  async function handleSubmit(event) {
    event.preventDefault()
    mutation.reset()

    try {
      await mutation.mutateAsync({
        email: email.trim().toLowerCase(),
        password,
        role,
      })
      onClose()
    } catch {
      // The normalized API error is displayed below.
    }
  }

  return (
    <Dialog
      fullWidth
      maxWidth="sm"
      onClose={mutation.isPending ? undefined : onClose}
      open
    >
      <Stack component="form" onSubmit={handleSubmit}>
        <DialogTitle>Create login account</DialogTitle>
        <DialogContent>
          <Stack spacing={2.5} sx={{ pt: 1 }}>
            {mutation.isError && (
              <Alert severity="error">{mutation.error.message}</Alert>
            )}
            <TextField
              autoComplete="email"
              autoFocus
              fullWidth
              label="Email address"
              onChange={(event) => setEmail(event.target.value)}
              required
              slotProps={{ htmlInput: { maxLength: 254 } }}
              type="email"
              value={email}
            />
            <TextField
              autoComplete="new-password"
              fullWidth
              helperText="Use between 8 and 72 characters."
              label="Password"
              onChange={(event) => setPassword(event.target.value)}
              required
              slotProps={{ htmlInput: { minLength: 8, maxLength: 72 } }}
              type="password"
              value={password}
            />
            <FormControl fullWidth required>
              <InputLabel id="new-user-role-label">Role</InputLabel>
              <Select
                label="Role"
                labelId="new-user-role-label"
                onChange={(event) => setRole(event.target.value)}
                value={role}
              >
                <MenuItem value="STAFF">Staff</MenuItem>
                <MenuItem value="RECEPTIONIST">Receptionist</MenuItem>
              </Select>
            </FormControl>
          </Stack>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 3 }}>
          <Button disabled={mutation.isPending} onClick={onClose}>
            Cancel
          </Button>
          <Button
            disabled={mutation.isPending}
            type="submit"
            variant="contained"
          >
            {mutation.isPending ? 'Creating…' : 'Create account'}
          </Button>
        </DialogActions>
      </Stack>
    </Dialog>
  )
}

UserFormDialog.propTypes = {
  onClose: PropTypes.func.isRequired,
}

export default UserFormDialog
