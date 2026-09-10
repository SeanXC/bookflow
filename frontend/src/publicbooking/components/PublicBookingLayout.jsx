import { Box, Container, Stack, Typography } from '@mui/material'
import { Outlet } from 'react-router-dom'

function PublicBookingLayout() {
  return (
    <Box
      component="main"
      sx={{
        minHeight: '100vh',
        py: { xs: 4, sm: 6 },
      }}
    >
      <Container maxWidth="md">
        <Stack spacing={4}>
          <Box
            sx={{
              alignItems: 'center',
              display: 'inline-flex',
              gap: 1.25,
            }}
          >
            <Box
              aria-hidden="true"
              sx={{
                bgcolor: 'primary.main',
                borderRadius: 1.5,
                color: 'primary.contrastText',
                display: 'grid',
                fontSize: 20,
                fontWeight: 800,
                height: 38,
                placeItems: 'center',
                width: 38,
              }}
            >
              B
            </Box>
            <Typography fontSize={22} fontWeight={800}>
              BookFlow
            </Typography>
          </Box>
          <Outlet />
        </Stack>
      </Container>
    </Box>
  )
}

export default PublicBookingLayout
