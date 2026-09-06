import { useState } from 'react'
import { Box } from '@mui/material'
import { Outlet } from 'react-router-dom'

import AppHeader from './AppHeader.jsx'
import AppSidebar from './AppSidebar.jsx'

function AppLayout() {
  const [mobileOpen, setMobileOpen] = useState(false)

  return (
    <Box sx={{ display: 'flex', minHeight: '100vh' }}>
      <AppSidebar
        mobileOpen={mobileOpen}
        onClose={() => setMobileOpen(false)}
      />
      <Box sx={{ display: 'flex', flex: 1, flexDirection: 'column', minWidth: 0 }}>
        <AppHeader onMenuOpen={() => setMobileOpen(true)} />
        <Box component="main" sx={{ flex: 1, p: { xs: 2, sm: 3 } }}>
          <Outlet />
        </Box>
      </Box>
    </Box>
  )
}

export default AppLayout
