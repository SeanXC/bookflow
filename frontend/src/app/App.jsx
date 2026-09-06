import {
  Box,
  Button,
  Chip,
  Container,
  Paper,
  Stack,
  Typography,
} from '@mui/material'
import { Link as RouterLink } from 'react-router-dom'

import { useAuth } from '../auth/context/useAuth.js'

function App() {
  const { isAuthenticated, user } = useAuth()

  return (
    <Box
      component="main"
      sx={{
        alignItems: 'center',
        display: 'flex',
        minHeight: '100vh',
        py: 4,
      }}
    >
      <Container maxWidth="sm">
        <Paper
          component="section"
          elevation={0}
          sx={{
            border: 1,
            borderColor: 'divider',
            borderRadius: 4,
            boxShadow: '0 24px 70px rgba(15, 23, 42, 0.1)',
            p: { xs: 4, sm: 8 },
            textAlign: 'center',
          }}
        >
          <Stack alignItems="center" spacing={2.5}>
            <Box
              aria-hidden="true"
              sx={{
                alignItems: 'center',
                bgcolor: 'primary.main',
                borderRadius: 2,
                color: 'primary.contrastText',
                display: 'flex',
                fontSize: 28,
                fontWeight: 800,
                height: 56,
                justifyContent: 'center',
                width: 56,
              }}
            >
              B
            </Box>
            <Typography color="primary" fontWeight={700} variant="overline">
              Appointment management, simplified
            </Typography>
            <Typography component="h1" variant="h1">
              BookFlow
            </Typography>
            <Typography color="text.secondary" sx={{ lineHeight: 1.7 }}>
              The application foundation is ready. Authentication and the
              tenant workspace come next.
            </Typography>
            {isAuthenticated ? (
              <Chip
                color="success"
                label={`Signed in as ${user?.email}`}
                variant="outlined"
              />
            ) : (
              <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1.5}>
                <Button component={RouterLink} to="/login" variant="contained">
                  Sign in
                </Button>
                <Button component={RouterLink} to="/register" variant="outlined">
                  Create account
                </Button>
              </Stack>
            )}
          </Stack>
        </Paper>
      </Container>
    </Box>
  )
}

export default App
