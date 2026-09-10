import { useState } from 'react'
import { Alert, Button, Stack, TextField, Typography } from '@mui/material'
import PropTypes from 'prop-types'

import { formatSlotDate, formatSlotTime } from '../format.js'

function PublicBookingForm({
  errorMessage,
  isSubmitting,
  onSubmit,
  selectedSlot,
}) {
  const [firstName, setFirstName] = useState('')
  const [lastName, setLastName] = useState('')
  const [email, setEmail] = useState('')
  const [phone, setPhone] = useState('')
  const [notes, setNotes] = useState('')

  /**
   * @param {import('react').FormEvent<HTMLFormElement>} event
   */
  function handleSubmit(event) {
    event.preventDefault()
    onSubmit({
      firstName: firstName.trim(),
      lastName: lastName.trim(),
      email: email.trim().toLowerCase(),
      phone: phone.trim(),
      notes: notes.trim() || null,
    })
  }

  return (
    <Stack component="form" onSubmit={handleSubmit} spacing={2.5}>
      <Typography component="h2" fontWeight={800} variant="h5">
        Your details
      </Typography>
      {selectedSlot ? (
        <Typography color="text.secondary">
          {formatSlotDate(selectedSlot.startTime)} at{' '}
          {formatSlotTime(selectedSlot.startTime)}
        </Typography>
      ) : (
        <Typography color="text.secondary">
          Select a time before completing your booking.
        </Typography>
      )}
      {errorMessage ? <Alert severity="error">{errorMessage}</Alert> : null}
      <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
        <TextField
          autoComplete="given-name"
          fullWidth
          label="First name"
          name="firstName"
          onChange={(event) => setFirstName(event.target.value)}
          required
          slotProps={{ htmlInput: { maxLength: 100 } }}
          value={firstName}
        />
        <TextField
          autoComplete="family-name"
          fullWidth
          label="Last name"
          name="lastName"
          onChange={(event) => setLastName(event.target.value)}
          required
          slotProps={{ htmlInput: { maxLength: 100 } }}
          value={lastName}
        />
      </Stack>
      <TextField
        autoComplete="email"
        fullWidth
        label="Email"
        name="email"
        onChange={(event) => setEmail(event.target.value)}
        required
        slotProps={{ htmlInput: { maxLength: 254 } }}
        type="email"
        value={email}
      />
      <TextField
        autoComplete="tel"
        fullWidth
        label="Phone"
        name="phone"
        onChange={(event) => setPhone(event.target.value)}
        required
        slotProps={{ htmlInput: { maxLength: 30 } }}
        type="tel"
        value={phone}
      />
      <TextField
        fullWidth
        label="Notes (optional)"
        minRows={3}
        multiline
        name="notes"
        onChange={(event) => setNotes(event.target.value)}
        value={notes}
      />
      <Button
        disabled={isSubmitting || !selectedSlot}
        size="large"
        type="submit"
        variant="contained"
      >
        {isSubmitting ? 'Booking…' : 'Confirm booking'}
      </Button>
    </Stack>
  )
}

PublicBookingForm.propTypes = {
  errorMessage: PropTypes.string,
  isSubmitting: PropTypes.bool.isRequired,
  onSubmit: PropTypes.func.isRequired,
  selectedSlot: PropTypes.shape({
    startTime: PropTypes.string.isRequired,
    endTime: PropTypes.string.isRequired,
  }),
}

export default PublicBookingForm
