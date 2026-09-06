import { useState } from 'react'
import {
  Alert,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  Stack,
  TextField,
} from '@mui/material'
import PropTypes from 'prop-types'

import {
  useCreateCustomer,
  useUpdateCustomer,
} from '../api/customerMutations.js'

function CustomerFormDialog({ customer, onClose }) {
  const createMutation = useCreateCustomer()
  const updateMutation = useUpdateCustomer()
  const [firstName, setFirstName] = useState(customer?.firstName ?? '')
  const [lastName, setLastName] = useState(customer?.lastName ?? '')
  const [email, setEmail] = useState(customer?.email ?? '')
  const [phone, setPhone] = useState(customer?.phone ?? '')
  const [notes, setNotes] = useState(customer?.notes ?? '')
  const [errorMessage, setErrorMessage] = useState('')
  const isSubmitting = createMutation.isPending || updateMutation.isPending

  /**
   * @param {import('react').FormEvent<HTMLFormElement>} event
   */
  async function handleSubmit(event) {
    event.preventDefault()
    setErrorMessage('')
    const request = {
      firstName: firstName.trim(),
      lastName: lastName.trim(),
      email: email.trim().toLowerCase() || null,
      phone: phone.trim() || null,
      notes: notes.trim() || null,
    }

    try {
      if (customer) {
        await updateMutation.mutateAsync({ customerId: customer.id, request })
      } else {
        await createMutation.mutateAsync(request)
      }
      onClose()
    } catch (error) {
      setErrorMessage(
        error instanceof Error ? error.message : 'Unable to save customer.',
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
      open
    >
      <Stack component="form" onSubmit={handleSubmit}>
        <DialogTitle>
          {customer ? 'Edit customer' : 'Create customer'}
        </DialogTitle>
        <DialogContent>
          <Stack spacing={2.5} sx={{ pt: 1 }}>
            {errorMessage && <Alert severity="error">{errorMessage}</Alert>}
            <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
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
            </Stack>
            <TextField
              fullWidth
              label="Email (optional)"
              onChange={(event) => setEmail(event.target.value)}
              slotProps={{ htmlInput: { maxLength: 254 } }}
              type="email"
              value={email}
            />
            <TextField
              fullWidth
              label="Phone (optional)"
              onChange={(event) => setPhone(event.target.value)}
              slotProps={{ htmlInput: { maxLength: 30 } }}
              type="tel"
              value={phone}
            />
            <TextField
              fullWidth
              label="Notes (optional)"
              minRows={4}
              multiline
              onChange={(event) => setNotes(event.target.value)}
              slotProps={{ htmlInput: { maxLength: 4000 } }}
              value={notes}
            />
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

CustomerFormDialog.propTypes = {
  customer: PropTypes.shape({
    id: PropTypes.number.isRequired,
    firstName: PropTypes.string.isRequired,
    lastName: PropTypes.string.isRequired,
    email: PropTypes.string,
    phone: PropTypes.string,
    notes: PropTypes.string,
  }),
  onClose: PropTypes.func.isRequired,
}

export default CustomerFormDialog
