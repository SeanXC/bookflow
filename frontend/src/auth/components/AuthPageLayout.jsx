import { Box, Container, Paper, Stack, Typography } from '@mui/material'
import PropTypes from 'prop-types'
import { Link as RouterLink } from 'react-router-dom'

function AuthPageLayout({ children, footer, subtitle, title }) {
  return (
    <Box
      component="main"
      sx={{
        alignItems: 'center',
        display: 'flex',
        minHeight: '100vh',
        py: 5,
      }}
    >
      <Container maxWidth="sm">
        <Stack alignItems="center" spacing={3}>
          <Box
            aria-label="BookFlow home"
            component={RouterLink}
            sx={{
              alignItems: 'center',
              color: 'text.primary',
              display: 'inline-flex',
              gap: 1.25,
              textDecoration: 'none',
            }}
            to="/"
          >
            <Box
              aria-hidden="true"
              sx={{
                display: 'grid',
                width: 42,
                height: 42,
                borderRadius: 1.5,
                bgcolor: 'primary.main',
                color: 'primary.contrastText',
                fontSize: 22,
                fontWeight: 800,
                placeItems: 'center',
              }}
            >
              B
            </Box>
            <Typography component="span" fontSize={24} fontWeight={800}>
              BookFlow
            </Typography>
          </Box>

          <Paper
            elevation={0}
            sx={{
              border: 1,
              borderColor: 'divider',
              borderRadius: 3,
              p: { xs: 3, sm: 5 },
              width: '100%',
            }}
          >
            <Stack spacing={3}>
              <Box>
                <Typography component="h1" fontWeight={800} variant="h4">
                  {title}
                </Typography>
                <Typography color="text.secondary" mt={1}>
                  {subtitle}
                </Typography>
              </Box>
              {children}
            </Stack>
          </Paper>

          <Typography color="text.secondary" variant="body2">
            {footer}
          </Typography>
        </Stack>
      </Container>
    </Box>
  )
}

AuthPageLayout.propTypes = {
  children: PropTypes.node.isRequired,
  footer: PropTypes.node.isRequired,
  subtitle: PropTypes.string.isRequired,
  title: PropTypes.string.isRequired,
}

export default AuthPageLayout
