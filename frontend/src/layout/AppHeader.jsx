import LogoutIcon from '@mui/icons-material/Logout'
import MenuIcon from '@mui/icons-material/Menu'
import {
  AppBar,
  Box,
  Button,
  Chip,
  IconButton,
  Toolbar,
  Typography,
} from '@mui/material'
import PropTypes from 'prop-types'
import { useNavigate } from 'react-router-dom'

import { useAuth } from '../auth/context/useAuth.js'

function AppHeader({ onMenuOpen }) {
  const { logout, user } = useAuth()
  const navigate = useNavigate()

  function handleLogout() {
    logout()
    navigate('/login', { replace: true })
  }

  return (
    <AppBar
      color="inherit"
      elevation={0}
      position="sticky"
      sx={{ borderBottom: 1, borderColor: 'divider' }}
    >
      <Toolbar sx={{ gap: 2 }}>
        <IconButton
          aria-label="Open navigation"
          edge="start"
          onClick={onMenuOpen}
          sx={{ display: { md: 'none' } }}
        >
          <MenuIcon />
        </IconButton>
        <Typography component="div" fontWeight={800} sx={{ flexGrow: 1 }}>
          Workspace
        </Typography>
        <Box
          sx={{
            alignItems: 'center',
            display: { xs: 'none', sm: 'flex' },
            gap: 1,
          }}
        >
          <Typography color="text.secondary" variant="body2">
            {user?.email}
          </Typography>
          <Chip label={user?.role} size="small" />
        </Box>
        <Button
          color="inherit"
          onClick={handleLogout}
          startIcon={<LogoutIcon />}
        >
          Sign out
        </Button>
      </Toolbar>
    </AppBar>
  )
}

AppHeader.propTypes = {
  onMenuOpen: PropTypes.func.isRequired,
}

export default AppHeader
