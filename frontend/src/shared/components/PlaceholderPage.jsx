import { Paper, Stack, Typography } from '@mui/material'
import PropTypes from 'prop-types'

function PlaceholderPage({ description, title }) {
  return (
    <Stack spacing={3}>
      <div>
        <Typography component="h1" fontWeight={800} variant="h4">
          {title}
        </Typography>
        <Typography color="text.secondary" mt={0.75}>
          {description}
        </Typography>
      </div>
      <Paper
        elevation={0}
        sx={{
          border: 1,
          borderColor: 'divider',
          borderRadius: 3,
          p: 4,
        }}
      >
        <Typography color="text.secondary">
          This workspace is ready for its feature implementation.
        </Typography>
      </Paper>
    </Stack>
  )
}

PlaceholderPage.propTypes = {
  description: PropTypes.string.isRequired,
  title: PropTypes.string.isRequired,
}

export default PlaceholderPage
