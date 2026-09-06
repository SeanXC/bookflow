import ArrowForwardIcon from '@mui/icons-material/ArrowForward'
import {
  Alert,
  Button,
  Chip,
  Paper,
  Skeleton,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Typography,
} from '@mui/material'
import { Link as RouterLink } from 'react-router-dom'

import { useAppointments } from '../../appointments/api/appointmentQueries.js'

const dateTimeFormatter = new Intl.DateTimeFormat('en-IE', {
  dateStyle: 'medium',
  timeStyle: 'short',
})

const statusColors = {
  CONFIRMED: 'primary',
  COMPLETED: 'success',
  CANCELLED: 'default',
}

function RecentAppointments() {
  const appointmentsQuery = useAppointments({
    page: 0,
    pageSize: 5,
    sortModel: [
      { field: 'startTime', sort: 'desc' },
      { field: 'id', sort: 'desc' },
    ],
  })

  return (
    <Paper
      elevation={0}
      sx={{ border: 1, borderColor: 'divider', borderRadius: 3, p: 3 }}
    >
      <Stack
        alignItems="center"
        direction="row"
        justifyContent="space-between"
        mb={2}
      >
        <Typography component="h2" fontWeight={700} variant="h6">
          Recent appointments
        </Typography>
        <Button
          component={RouterLink}
          endIcon={<ArrowForwardIcon />}
          size="small"
          to="/appointments"
        >
          View all
        </Button>
      </Stack>

      {appointmentsQuery.isError ? (
        <Alert severity="error">{appointmentsQuery.error.message}</Alert>
      ) : (
        <TableContainer>
          <Table aria-label="Recent appointments">
            <TableHead>
              <TableRow>
                <TableCell>Date and time</TableCell>
                <TableCell>Customer</TableCell>
                <TableCell>Service</TableCell>
                <TableCell>Staff</TableCell>
                <TableCell>Status</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {appointmentsQuery.isPending
                ? Array.from({ length: 3 }, (_, index) => (
                    <TableRow key={index}>
                      {Array.from({ length: 5 }, (_value, cellIndex) => (
                        <TableCell key={cellIndex}>
                          <Skeleton />
                        </TableCell>
                      ))}
                    </TableRow>
                  ))
                : appointmentsQuery.data.content.map((appointment) => (
                    <TableRow hover key={appointment.id}>
                      <TableCell>
                        {dateTimeFormatter.format(
                          new Date(appointment.startTime),
                        )}
                      </TableCell>
                      <TableCell>
                        {appointment.customer.firstName}{' '}
                        {appointment.customer.lastName}
                      </TableCell>
                      <TableCell>{appointment.service.name}</TableCell>
                      <TableCell>
                        {appointment.staff.firstName}{' '}
                        {appointment.staff.lastName}
                      </TableCell>
                      <TableCell>
                        <Chip
                          color={
                            statusColors[appointment.status] ?? 'default'
                          }
                          label={appointment.status}
                          size="small"
                        />
                      </TableCell>
                    </TableRow>
                  ))}
              {!appointmentsQuery.isPending &&
                appointmentsQuery.data.content.length === 0 && (
                  <TableRow>
                    <TableCell align="center" colSpan={5}>
                      No appointments yet.
                    </TableCell>
                  </TableRow>
                )}
            </TableBody>
          </Table>
        </TableContainer>
      )}
    </Paper>
  )
}

export default RecentAppointments
