import { Box, CircularProgress } from '@mui/material'

function AppLoading() {
  return (
    <Box
      aria-label="Loading application"
      role="status"
      sx={{
        display: 'grid',
        minHeight: '100vh',
        placeItems: 'center',
      }}
    >
      <CircularProgress />
    </Box>
  )
}

export default AppLoading
