import {
  Alert,
  Box,
  Button,
  CircularProgress,
  Stack,
  TextField,
  Typography,
} from '@mui/material'
import PropTypes from 'prop-types'

import {
  formatSlotDate,
  formatSlotTime,
  groupSlotsByDay,
} from '../format.js'

function PublicSlotPicker({
  from,
  onRangeChange,
  onSelect,
  rangeError,
  selectedStartTime,
  slotsError,
  slotsLoading,
  slots,
  to,
}) {
  const groupedSlots = groupSlotsByDay(slots)

  return (
    <Stack spacing={2}>
      <Typography component="h2" fontWeight={800} variant="h5">
        Choose a time
      </Typography>
      <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
        <TextField
          label="From"
          onChange={(event) =>
            onRangeChange({ from: event.target.value, to })
          }
          type="date"
          value={from}
        />
        <TextField
          label="To"
          onChange={(event) =>
            onRangeChange({ from, to: event.target.value })
          }
          type="date"
          value={to}
        />
      </Stack>
      {rangeError ? (
        <Alert severity="warning">{rangeError}</Alert>
      ) : slotsError ? (
        <Alert severity="error">{slotsError}</Alert>
      ) : slotsLoading ? (
        <Box sx={{ display: 'grid', minHeight: 120, placeItems: 'center' }}>
          <CircularProgress aria-label="Loading available times" />
        </Box>
      ) : groupedSlots.length === 0 ? (
        <Typography color="text.secondary">
          No available times in this date range.
        </Typography>
      ) : (
        groupedSlots.map(([day, daySlots]) => (
          <Stack key={day} spacing={1.25}>
            <Typography fontWeight={700}>
              {formatSlotDate(daySlots[0].startTime)}
            </Typography>
            <Box sx={{ display: 'flex', flexWrap: 'wrap', gap: 1 }}>
              {daySlots.map((slot) => (
                <Button
                  key={slot.startTime}
                  onClick={() => onSelect(slot)}
                  variant={
                    selectedStartTime === slot.startTime
                      ? 'contained'
                      : 'outlined'
                  }
                >
                  {formatSlotTime(slot.startTime)}
                </Button>
              ))}
            </Box>
          </Stack>
        ))
      )}
    </Stack>
  )
}

PublicSlotPicker.propTypes = {
  from: PropTypes.string.isRequired,
  onRangeChange: PropTypes.func.isRequired,
  onSelect: PropTypes.func.isRequired,
  rangeError: PropTypes.string,
  selectedStartTime: PropTypes.string,
  slotsError: PropTypes.string,
  slotsLoading: PropTypes.bool.isRequired,
  slots: PropTypes.arrayOf(
    PropTypes.shape({
      startTime: PropTypes.string.isRequired,
      endTime: PropTypes.string.isRequired,
    }).isRequired,
  ).isRequired,
  to: PropTypes.string.isRequired,
}

export default PublicSlotPicker
