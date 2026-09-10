import { Button, Stack, Typography } from '@mui/material'
import PropTypes from 'prop-types'

function PublicUnavailableState({ onRetry }) {
  return (
    <Stack spacing={2}>
      <Typography component="h1" fontWeight={800} variant="h4">
        Booking page unavailable
      </Typography>
      <Typography color="text.secondary">
        This business is not accepting online bookings right now.
      </Typography>
      {onRetry ? (
        <Button onClick={onRetry} sx={{ alignSelf: 'flex-start' }}>
          Try again
        </Button>
      ) : null}
    </Stack>
  )
}

PublicUnavailableState.propTypes = {
  onRetry: PropTypes.func,
}

export default PublicUnavailableState
