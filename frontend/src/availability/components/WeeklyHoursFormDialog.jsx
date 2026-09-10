import { useState } from 'react'
import {
  Alert,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  FormControl,
  FormHelperText,
  InputLabel,
  MenuItem,
  Select,
  Stack,
  TextField,
} from '@mui/material'
import PropTypes from 'prop-types'

import {
  useCreateWeeklyHours,
  useUpdateWeeklyHours,
} from '../api/availabilityMutations.js'
import { DAYS_OF_WEEK } from '../constants.js'
import { toApiTime, toTimeInputValue } from '../format.js'

function WeeklyHoursFormDialog({ hours, onClose, staffId }) {
  const createMutation = useCreateWeeklyHours()
  const updateMutation = useUpdateWeeklyHours()
  const [dayOfWeek, setDayOfWeek] = useState(hours?.dayOfWeek ?? 'MONDAY')
  const [startTime, setStartTime] = useState(
    toTimeInputValue(hours?.startTime ?? '09:00:00'),
  )
  const [endTime, setEndTime] = useState(
    toTimeInputValue(hours?.endTime ?? '17:00:00'),
  )
  const [submitted, setSubmitted] = useState(false)
  const isEditing = hours !== null
  const isSubmitting = createMutation.isPending || updateMutation.isPending
  const mutation = isEditing ? updateMutation : createMutation
  const timeRangeInvalid = !startTime || !endTime || startTime >= endTime

  async function handleSubmit(event) {
    event.preventDefault()
    setSubmitted(true)
    mutation.reset()
    if (timeRangeInvalid) {
      return
    }

    const request = {
      dayOfWeek,
      startTime: toApiTime(startTime),
      endTime: toApiTime(endTime),
    }

    try {
      if (hours) {
        await updateMutation.mutateAsync({
          staffId,
          hoursId: hours.id,
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
          {isEditing ? 'Edit weekly hours' : 'Add weekly hours'}
        </DialogTitle>
        <DialogContent>
          <Stack spacing={2.5} sx={{ pt: 1 }}>
            {mutation.isError && (
              <Alert severity="error">{mutation.error.message}</Alert>
            )}
            <FormControl fullWidth required>
              <InputLabel id="weekly-hours-day-label">Day</InputLabel>
              <Select
                label="Day"
                labelId="weekly-hours-day-label"
                onChange={(event) => setDayOfWeek(event.target.value)}
                value={dayOfWeek}
              >
                {DAYS_OF_WEEK.map((day) => (
                  <MenuItem key={day.value} value={day.value}>
                    {day.label}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
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
            {submitted && timeRangeInvalid && (
              <FormHelperText error>
                End time must be after start time.
              </FormHelperText>
            )}
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

WeeklyHoursFormDialog.propTypes = {
  hours: PropTypes.shape({
    id: PropTypes.number.isRequired,
    dayOfWeek: PropTypes.string.isRequired,
    startTime: PropTypes.string.isRequired,
    endTime: PropTypes.string.isRequired,
  }),
  onClose: PropTypes.func.isRequired,
  staffId: PropTypes.number.isRequired,
}

export default WeeklyHoursFormDialog
