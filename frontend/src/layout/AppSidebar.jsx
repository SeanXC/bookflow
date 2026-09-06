import CalendarMonthOutlinedIcon from '@mui/icons-material/CalendarMonthOutlined'
import DashboardOutlinedIcon from '@mui/icons-material/DashboardOutlined'
import DesignServicesOutlinedIcon from '@mui/icons-material/DesignServicesOutlined'
import GroupsOutlinedIcon from '@mui/icons-material/GroupsOutlined'
import PeopleOutlineIcon from '@mui/icons-material/PeopleOutline'
import PersonOutlineIcon from '@mui/icons-material/PersonOutline'
import {
  Box,
  Divider,
  Drawer,
  List,
  ListItemButton,
  ListItemIcon,
  ListItemText,
  Typography,
} from '@mui/material'
import PropTypes from 'prop-types'
import { NavLink, useLocation } from 'react-router-dom'

import { useAuth } from '../auth/context/useAuth.js'
import { DRAWER_WIDTH, navigationItems } from './navigation.js'

const icons = {
  dashboard: <DashboardOutlinedIcon />,
  appointments: <CalendarMonthOutlinedIcon />,
  customers: <PeopleOutlineIcon />,
  staff: <GroupsOutlinedIcon />,
  services: <DesignServicesOutlinedIcon />,
  users: <PersonOutlineIcon />,
}

function AppSidebar({ mobileOpen, onClose }) {
  const { user } = useAuth()
  const location = useLocation()
  const visibleItems = navigationItems.filter(
    (item) => user && item.roles.includes(user.role),
  )

  const content = (
    <>
      <Box sx={{ alignItems: 'center', display: 'flex', gap: 1.25, px: 3, py: 2.5 }}>
        <Box
          aria-hidden="true"
          sx={{
            display: 'grid',
            width: 38,
            height: 38,
            borderRadius: 1.5,
            bgcolor: 'primary.main',
            color: 'primary.contrastText',
            fontSize: 20,
            fontWeight: 800,
            placeItems: 'center',
          }}
        >
          B
        </Box>
        <Typography fontSize={22} fontWeight={800}>
          BookFlow
        </Typography>
      </Box>
      <Divider />
      <List component="nav" sx={{ p: 1.5 }}>
        {visibleItems.map((item) => {
          const selected =
            location.pathname === item.path ||
            location.pathname.startsWith(`${item.path}/`)
          const label =
            user?.role === 'STAFF' && item.staffLabel
              ? item.staffLabel
              : item.label

          return (
            <ListItemButton
              component={NavLink}
              key={item.path}
              onClick={onClose}
              selected={selected}
              sx={{ borderRadius: 1.5, mb: 0.5 }}
              to={item.path}
            >
              <ListItemIcon sx={{ minWidth: 40 }}>
                {icons[item.icon]}
              </ListItemIcon>
              <ListItemText primary={label} />
            </ListItemButton>
          )
        })}
      </List>
    </>
  )

  const paperStyles = {
    boxSizing: 'border-box',
    width: DRAWER_WIDTH,
  }

  return (
    <>
      <Drawer
        onClose={onClose}
        open={mobileOpen}
        slotProps={{ paper: { sx: paperStyles } }}
        sx={{ display: { xs: 'block', md: 'none' } }}
        variant="temporary"
      >
        {content}
      </Drawer>
      <Drawer
        open
        slotProps={{ paper: { sx: paperStyles } }}
        sx={{
          display: { xs: 'none', md: 'block' },
          flexShrink: 0,
          width: DRAWER_WIDTH,
          '& .MuiDrawer-paper': {
            position: 'relative',
          },
        }}
        variant="permanent"
      >
        {content}
      </Drawer>
    </>
  )
}

AppSidebar.propTypes = {
  mobileOpen: PropTypes.bool.isRequired,
  onClose: PropTypes.func.isRequired,
}

export default AppSidebar
