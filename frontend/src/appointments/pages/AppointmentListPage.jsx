import { useMemo, useState } from 'react'
import AddIcon from '@mui/icons-material/Add'
import CancelOutlinedIcon from '@mui/icons-material/CancelOutlined'
import CheckCircleOutlineIcon from '@mui/icons-material/CheckCircleOutline'
import ClearIcon from '@mui/icons-material/Clear'
import EditOutlinedIcon from '@mui/icons-material/EditOutlined'
import {
  Alert,
  Box,
  Button,
  Chip,
  FormControl,
  InputLabel,
  MenuItem,
  Paper,
  Select,
  Stack,
  TextField,
  Typography,
} from '@mui/material'
import { DataGrid } from '@mui/x-data-grid'

import { useAuth } from '../../auth/context/useAuth.js'
import { useStaff } from '../../staff/api/staffQueries.js'
import { useAppointments } from '../api/appointmentQueries.js'
import AppointmentFormDialog from '../components/AppointmentFormDialog.jsx'
import AppointmentStatusDialog from '../components/AppointmentStatusDialog.jsx'

const INITIAL_PAGINATION = {
  page: 0,
  pageSize: 20,
}

const INITIAL_SORT = [
  {
    field: 'startTime',
    sort: 'desc',
  },
]

const dateTimeFormatter = new Intl.DateTimeFormat('en-IE', {
  dateStyle: 'medium',
  timeStyle: 'short',
})

const currencyFormatter = new Intl.NumberFormat('en-IE', {
  style: 'currency',
  currency: 'EUR',
})

const statusColors = {
  CONFIRMED: 'primary',
  COMPLETED: 'success',
  CANCELLED: 'default',
}

/**
 * Converts a browser-local date and time to the backend's UTC format.
 *
 * @param {string} value
 * @returns {string | undefined}
 */
function toUtcInstant(value) {
  return value ? new Date(value).toISOString() : undefined
}

function AppointmentListPage() {
  const { user } = useAuth()
  const canManageAppointments =
    user?.role === 'OWNER' || user?.role === 'RECEPTIONIST'
  const canFilterByStaff = user?.role !== 'STAFF'
  const [staffId, setStaffId] = useState('all')
  const [status, setStatus] = useState('all')
  const [from, setFrom] = useState('')
  const [to, setTo] = useState('')
  const [paginationModel, setPaginationModel] = useState(INITIAL_PAGINATION)
  const [sortModel, setSortModel] = useState(INITIAL_SORT)
  const [formOpen, setFormOpen] = useState(false)
  const [editingAppointment, setEditingAppointment] = useState(
    /** @type {import('../types.js').Appointment | null} */ (null),
  )
  const [statusAction, setStatusAction] = useState(
    /** @type {{
     *   appointment: import('../types.js').Appointment,
     *   status: 'COMPLETED' | 'CANCELLED'
     * } | null} */ (null),
  )

  const staffQuery = useStaff(
    {
      active: true,
      page: 0,
      pageSize: 100,
      sortModel: [{ field: 'lastName', sort: 'asc' }],
    },
    canFilterByStaff,
  )

  const appointmentsQuery = useAppointments({
    ...(canFilterByStaff && staffId !== 'all'
      ? { staffId: Number(staffId) }
      : {}),
    ...(status !== 'all' ? { status } : {}),
    ...(from ? { from: toUtcInstant(from) } : {}),
    ...(to ? { to: toUtcInstant(to) } : {}),
    page: paginationModel.page,
    pageSize: paginationModel.pageSize,
    sortModel,
  })

  /** @type {import('@mui/x-data-grid').GridColDef<
   *   import('../types.js').Appointment>[]}
   */
  const columns = useMemo(() => {
    const baseColumns = [
      {
        field: 'startTime',
        headerName: 'Starts',
        minWidth: 190,
        flex: 1,
        valueFormatter: (value) => dateTimeFormatter.format(new Date(value)),
      },
      {
        field: 'endTime',
        headerName: 'Ends',
        minWidth: 190,
        flex: 1,
        sortable: false,
        valueFormatter: (value) => dateTimeFormatter.format(new Date(value)),
      },
      {
        field: 'customer',
        headerName: 'Customer',
        minWidth: 180,
        flex: 1,
        sortable: false,
        valueGetter: (value) => `${value.firstName} ${value.lastName}`,
      },
      {
        field: 'service',
        headerName: 'Service',
        minWidth: 180,
        flex: 1,
        sortable: false,
        valueGetter: (value) => value.name,
      },
      {
        field: 'staff',
        headerName: 'Staff',
        minWidth: 180,
        flex: 1,
        sortable: false,
        valueGetter: (value) => `${value.firstName} ${value.lastName}`,
      },
      {
        field: 'price',
        headerName: 'Price',
        minWidth: 120,
        sortable: false,
        valueGetter: (_value, row) => row.service.price,
        valueFormatter: (value) => currencyFormatter.format(value),
      },
      {
        field: 'status',
        headerName: 'Status',
        minWidth: 130,
        renderCell: (params) => (
          <Chip
            color={statusColors[params.value] ?? 'default'}
            label={params.value}
            size="small"
          />
        ),
      },
    ]

    baseColumns.push({
      field: 'actions',
      headerName: 'Actions',
      minWidth: canManageAppointments ? 330 : 240,
      sortable: false,
      filterable: false,
      renderCell: (params) =>
        params.row.status === 'CONFIRMED' ? (
          <Stack direction="row" spacing={0.5}>
            {canManageAppointments && (
              <Button
                onClick={() => {
                  setEditingAppointment(params.row)
                  setFormOpen(true)
                }}
                size="small"
                startIcon={<EditOutlinedIcon />}
              >
                Edit
              </Button>
            )}
            <Button
              color="success"
              onClick={() =>
                setStatusAction({
                  appointment: params.row,
                  status: 'COMPLETED',
                })
              }
              size="small"
              startIcon={<CheckCircleOutlineIcon />}
            >
              Complete
            </Button>
            <Button
              color="error"
              onClick={() =>
                setStatusAction({
                  appointment: params.row,
                  status: 'CANCELLED',
                })
              }
              size="small"
              startIcon={<CancelOutlinedIcon />}
            >
              Cancel
            </Button>
          </Stack>
        ) : null,
    })

    return baseColumns
  }, [canManageAppointments])

  function resetToFirstPage() {
    setPaginationModel((current) => ({ ...current, page: 0 }))
  }

  function clearFilters() {
    setStaffId('all')
    setStatus('all')
    setFrom('')
    setTo('')
    resetToFirstPage()
  }

  const hasFilters =
    (canFilterByStaff && staffId !== 'all') ||
    status !== 'all' ||
    from !== '' ||
    to !== ''

  return (
    <Stack spacing={3}>
      <Box
        sx={{
          alignItems: { sm: 'center' },
          display: 'flex',
          flexDirection: { xs: 'column', sm: 'row' },
          gap: 2,
          justifyContent: 'space-between',
        }}
      >
        <div>
          <Typography component="h1" fontWeight={800} variant="h4">
            Appointments
          </Typography>
          <Typography color="text.secondary" mt={0.75}>
            Review the appointment schedule. Times are shown in your local
            time.
          </Typography>
        </div>
        {canManageAppointments && (
          <Button
            onClick={() => {
              setEditingAppointment(null)
              setFormOpen(true)
            }}
            startIcon={<AddIcon />}
            variant="contained"
          >
            Create appointment
          </Button>
        )}
      </Box>

      {canFilterByStaff && staffQuery.isError && (
        <Alert severity="warning">
          Staff filters could not be loaded. Appointments are still available.
        </Alert>
      )}

      <Paper
        elevation={0}
        sx={{ border: 1, borderColor: 'divider', borderRadius: 3, p: 2 }}
      >
        <Stack
          alignItems={{ sm: 'center' }}
          direction={{ xs: 'column', sm: 'row' }}
          flexWrap="wrap"
          mb={2}
          spacing={2}
          useFlexGap
        >
          {canFilterByStaff && (
            <FormControl sx={{ minWidth: 210 }}>
              <InputLabel id="appointment-staff-label">Staff</InputLabel>
              <Select
                label="Staff"
                labelId="appointment-staff-label"
                onChange={(event) => {
                  setStaffId(event.target.value)
                  resetToFirstPage()
                }}
                value={staffId}
              >
                <MenuItem value="all">All staff</MenuItem>
                {(staffQuery.data?.content ?? []).map((staff) => (
                  <MenuItem key={staff.id} value={staff.id}>
                    {staff.firstName} {staff.lastName}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
          )}

          <FormControl sx={{ minWidth: 170 }}>
            <InputLabel id="appointment-status-label">Status</InputLabel>
            <Select
              label="Status"
              labelId="appointment-status-label"
              onChange={(event) => {
                setStatus(event.target.value)
                resetToFirstPage()
              }}
              value={status}
            >
              <MenuItem value="all">All statuses</MenuItem>
              <MenuItem value="CONFIRMED">Confirmed</MenuItem>
              <MenuItem value="COMPLETED">Completed</MenuItem>
              <MenuItem value="CANCELLED">Cancelled</MenuItem>
            </Select>
          </FormControl>

          <TextField
            label="From"
            onChange={(event) => {
              setFrom(event.target.value)
              resetToFirstPage()
            }}
            slotProps={{ inputLabel: { shrink: true } }}
            type="datetime-local"
            value={from}
          />
          <TextField
            label="To"
            onChange={(event) => {
              setTo(event.target.value)
              resetToFirstPage()
            }}
            slotProps={{ inputLabel: { shrink: true } }}
            type="datetime-local"
            value={to}
          />
          <Button
            disabled={!hasFilters}
            onClick={clearFilters}
            startIcon={<ClearIcon />}
          >
            Clear
          </Button>
        </Stack>

        {appointmentsQuery.isError ? (
          <Alert
            action={
              <Button
                color="inherit"
                onClick={() => appointmentsQuery.refetch()}
                size="small"
              >
                Retry
              </Button>
            }
            severity="error"
          >
            {appointmentsQuery.error.message}
          </Alert>
        ) : (
          <DataGrid
            autoHeight
            columns={columns}
            disableRowSelectionOnClick
            loading={appointmentsQuery.isFetching}
            onPaginationModelChange={setPaginationModel}
            onSortModelChange={(nextSortModel) => {
              setSortModel(nextSortModel)
              resetToFirstPage()
            }}
            pageSizeOptions={[10, 20, 50, 100]}
            paginationMode="server"
            paginationModel={paginationModel}
            rowCount={appointmentsQuery.data?.totalElements ?? 0}
            rows={appointmentsQuery.data?.content ?? []}
            sortingMode="server"
            sortModel={sortModel}
          />
        )}
      </Paper>

      {canManageAppointments && formOpen && (
        <AppointmentFormDialog
          appointment={editingAppointment}
          onClose={() => setFormOpen(false)}
          open
        />
      )}
      {statusAction && (
        <AppointmentStatusDialog
          appointment={statusAction.appointment}
          onClose={() => setStatusAction(null)}
          status={statusAction.status}
        />
      )}
    </Stack>
  )
}

export default AppointmentListPage
