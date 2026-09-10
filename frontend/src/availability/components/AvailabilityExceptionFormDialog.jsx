import { useState } from 'react'
import {
  Alert,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControl,
  FormControlLabel,
  FormHelperText,
  InputLabel,
  MenuItem,
  Select,
  Stack,
  Switch,
  TextField,
} from '@mui/material'
import PropTypes from 'prop-types'

import {
  useCreateAvailabilityException,
  useUpdateAvailabilityException,
} from '../api/availabilityMutations.js'
import { EXCEPTION_TYPES } from '../constants.js'
import { formatLocalDate, toApiTime, toTimeInputValue } from '../format.js'

function AvailabilityExceptionFormDialog({ exception, onClose, staffId }) {
  const createMutation = useCreateAvailabilityException()
  const updateMutation = useUpdateAvailabilityException()
  const [exceptionDate, setExceptionDate] = useState(
    exception?.exceptionDate ?? formatLocalDate(new Date()),
  )
  const [type, setType] = useState(exception?.type ?? 'UNAVAILABLE')
  const [allDay, setAllDay] = useState(
    exception
      ? exception.type === 'UNAVAILABLE' && exception.startTime == null
      : true,
  )
  const [startTime, setStartTime] = useState(
    toTimeInputValue(exception?.startTime ?? '09:00:00'),
  )
  const [endTime, setEndTime] = useState(
    toTimeInputValue(exception?.endTime ?? '17:00:00'),
  )
  const [note, setNote] = useState(exception?.note ?? '')
  const [submitted, setSubmitted] = useState(false)
  const isEditing = exception !== null
  const isSubmitting = createMutation.isPending || updateMutation.isPending
  const mutation = isEditing ? updateMutation : createMutation
  const needsTimes = type === 'CUSTOM_HOURS' || !allDay
  const timeRangeInvalid = needsTimes && (!startTime || !endTime || startTime >= endTime)

  /**
   * @param {import('../types.js').AvailabilityExceptionType} nextType
   */
  function handleTypeChange(nextType) {
    setType(nextType)
    if (nextType === 'CUSTOM_HOURS') {
      setAllDay(false)
    }
  }

  async function handleSubmit(event) {
    event.preventDefault()
    setSubmitted(true)
    mutation.reset()
    if (!exceptionDate || timeRangeInvalid) {
      return
    }

    const request = {
      exceptionDate,
      type,
      startTime: needsTimes ? toApiTime(startTime) : null,
      endTime: needsTimes ? toApiTime(endTime) : null,
      note: note.trim() || null,
    }

    try {
      if (exception) {
        await updateMutation.mutateAsync({
          staffId,
          exceptionId: exception.id,
          request,
        })
      } else {
        await createMutation.mutateAsync({ staffId, request })
      }
      onClose()
    } catch {
      // The normalized API error is displayed below.
    }
  }

  return (
    <Dialog
      fullWidth
      maxWidth="sm"
      onClose={isSubmitting ? undefined : onClose}
      open
    >
      <Stack component="form" onSubmit={handleSubmit}>
        <DialogTitle>
          {isEditing ? 'Edit exception' : 'Add exception'}
        </DialogTitle>
        <DialogContent>
          <Stack spacing={2.5} sx={{ pt: 1 }}>
            {mutation.isError && (
              <Alert severity="error">{mutation.error.message}</Alert>
            )}
            <TextField
              fullWidth
              label="Date"
              onChange={(event) => setExceptionDate(event.target.value)}
              required
              type="date"
              value={exceptionDate}
            />
            <FormControl fullWidth required>
              <InputLabel id="exception-type-label">Type</InputLabel>
              <Select
                label="Type"
                labelId="exception-type-label"
                onChange={(event) => handleTypeChange(event.target.value)}
                value={type}
              >
                {EXCEPTION_TYPES.map((item) => (
                  <MenuItem key={item.value} value={item.value}>
                    {item.label}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
            {type === 'UNAVAILABLE' && (
              <FormControlLabel
                control={
                  <Switch
                    checked={allDay}
                    onChange={(event) => setAllDay(event.target.checked)}
                  />
                }
                label="All day"
              />
            )}
            {needsTimes && (
              <>
                <TextField
                  fullWidth
                  label="Start time"
                  onChange={(event) => setStartTime(event.target.value)}
                  required
                  type="time"
                  value={startTime}
                />
                <TextField
                  error={submitted && timeRangeInvalid}
                  fullWidth
                  label="End time"
                  onChange={(event) => setEndTime(event.target.value)}
                  required
                  type="time"
                  value={endTime}
                />
              </>
            )}
            {submitted && timeRangeInvalid && (
              <FormHelperText error>
                End time must be after start time.
              </FormHelperText>
            )}
            <TextField
              fullWidth
              label="Note (optional)"
              onChange={(event) => setNote(event.target.value)}
              slotProps={{ htmlInput: { maxLength: 255 } }}
              value={note}
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

AvailabilityExceptionFormDialog.propTypes = {
  exception: PropTypes.shape({
    id: PropTypes.number.isRequired,
    exceptionDate: PropTypes.string.isRequired,
    type: PropTypes.string.isRequired,
    startTime: PropTypes.string,
    endTime: PropTypes.string,
    note: PropTypes.string,
  }),
  onClose: PropTypes.func.isRequired,
  staffId: PropTypes.number.isRequired,
}

export default AvailabilityExceptionFormDialog
