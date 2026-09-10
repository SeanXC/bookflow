import { useMemo, useState } from 'react'
import AddIcon from '@mui/icons-material/Add'
import ArrowBackIcon from '@mui/icons-material/ArrowBack'
import DeleteOutlineIcon from '@mui/icons-material/DeleteOutline'
import EditOutlinedIcon from '@mui/icons-material/EditOutlined'
import {
  Alert,
  Box,
  Button,
  Chip,
  CircularProgress,
  Paper,
  Stack,
  TextField,
  Typography,
} from '@mui/material'
import { DataGrid } from '@mui/x-data-grid'
import { Link as RouterLink, useParams } from 'react-router-dom'

import { useStaffById } from '../../staff/api/staffQueries.js'
import {
  useAvailabilityExceptions,
  useWeeklyHours,
} from '../api/availabilityQueries.js'
import {
  useDeleteAvailabilityException,
  useDeleteWeeklyHours,
} from '../api/availabilityMutations.js'
import { DAY_ORDER, MAX_EXCEPTION_RANGE_DAYS } from '../constants.js'
import AvailabilityExceptionFormDialog from '../components/AvailabilityExceptionFormDialog.jsx'
import DeleteAvailabilityDialog from '../components/DeleteAvailabilityDialog.jsx'
import WeeklyHoursFormDialog from '../components/WeeklyHoursFormDialog.jsx'
import {
  formatDayOfWeek,
  formatExceptionType,
  formatLocalDate,
  formatTime,
  getExceptionRangeError,
} from '../format.js'

const dateFormatter = new Intl.DateTimeFormat('en-IE', {
  dateStyle: 'medium',
})

function defaultExceptionRange() {
  const fromDate = new Date()
  const toDate = new Date()
  toDate.setDate(toDate.getDate() + (MAX_EXCEPTION_RANGE_DAYS - 1))
  return {
    from: formatLocalDate(fromDate),
    to: formatLocalDate(toDate),
  }
}

function StaffAvailabilityPage() {
  const { staffId: staffIdParam } = useParams()
  const staffId = Number(staffIdParam)
  const hasValidStaffId = Number.isInteger(staffId) && staffId > 0
  const [exceptionRange, setExceptionRange] = useState(defaultExceptionRange)
  const [hoursFormOpen, setHoursFormOpen] = useState(false)
  const [editingHours, setEditingHours] = useState(
    /** @type {import('../types.js').WeeklyHours | null} */ (null),
  )
  const [hoursToDelete, setHoursToDelete] = useState(
    /** @type {import('../types.js').WeeklyHours | null} */ (null),
  )
  const [exceptionFormOpen, setExceptionFormOpen] = useState(false)
  const [editingException, setEditingException] = useState(
    /** @type {import('../types.js').AvailabilityException | null} */ (null),
  )
  const [exceptionToDelete, setExceptionToDelete] = useState(
    /** @type {import('../types.js').AvailabilityException | null} */ (null),
  )
  const deleteHoursMutation = useDeleteWeeklyHours()
  const deleteExceptionMutation = useDeleteAvailabilityException()
  const rangeError = getExceptionRangeError(
    exceptionRange.from,
    exceptionRange.to,
  )

  const staffQuery = useStaffById(staffId, hasValidStaffId)
  const weeklyHoursQuery = useWeeklyHours(staffId, hasValidStaffId)
  const exceptionsQuery = useAvailabilityExceptions(
    {
      staffId,
      from: exceptionRange.from,
      to: exceptionRange.to,
    },
    hasValidStaffId && rangeError === null,
  )

  const weeklyHoursRows = useMemo(
    () =>
      [...(weeklyHoursQuery.data ?? [])].sort((left, right) => {
        const dayDiff =
          (DAY_ORDER[left.dayOfWeek] ?? 0) - (DAY_ORDER[right.dayOfWeek] ?? 0)
        return dayDiff !== 0
          ? dayDiff
          : left.startTime.localeCompare(right.startTime)
      }),
    [weeklyHoursQuery.data],
  )

  /** @type {import('@mui/x-data-grid').GridColDef<
   *   import('../types.js').WeeklyHours>[]}
   */
  const weeklyHoursColumns = useMemo(
    () => [
      {
        field: 'dayOfWeek',
        headerName: 'Day',
        minWidth: 150,
        flex: 1,
        valueFormatter: (value) => formatDayOfWeek(value),
      },
      {
        field: 'startTime',
        headerName: 'Starts',
        minWidth: 120,
        valueFormatter: (value) => formatTime(value),
      },
      {
        field: 'endTime',
        headerName: 'Ends',
        minWidth: 120,
        valueFormatter: (value) => formatTime(value),
      },
      {
        field: 'actions',
        headerName: 'Actions',
        minWidth: 180,
        sortable: false,
        filterable: false,
        renderCell: (params) => (
          <Stack direction="row" spacing={0.5}>
            <Button
              onClick={() => {
                setEditingHours(params.row)
                setHoursFormOpen(true)
              }}
              size="small"
              startIcon={<EditOutlinedIcon />}
            >
              Edit
            </Button>
            <Button
              color="error"
              onClick={() => setHoursToDelete(params.row)}
              size="small"
              startIcon={<DeleteOutlineIcon />}
            >
              Delete
            </Button>
          </Stack>
        ),
      },
    ],
    [],
  )

  /** @type {import('@mui/x-data-grid').GridColDef<
   *   import('../types.js').AvailabilityException>[]}
   */
  const exceptionColumns = useMemo(
    () => [
      {
        field: 'exceptionDate',
        headerName: 'Date',
        minWidth: 160,
        flex: 0.8,
        valueFormatter: (value) =>
          dateFormatter.format(new Date(`${value}T00:00:00`)),
      },
      {
        field: 'type',
        headerName: 'Type',
        minWidth: 160,
        renderCell: (params) => (
          <Chip
            color={params.value === 'UNAVAILABLE' ? 'default' : 'primary'}
            label={formatExceptionType(params.value)}
            size="small"
            variant="outlined"
          />
        ),
      },
      {
        field: 'startTime',
        headerName: 'Window',
        minWidth: 160,
        flex: 0.8,
        sortable: false,
        valueGetter: (_value, row) =>
          row.startTime == null
            ? 'All day'
            : `${formatTime(row.startTime)}–${formatTime(row.endTime)}`,
      },
      {
        field: 'note',
        headerName: 'Note',
        minWidth: 180,
        flex: 1,
        sortable: false,
        valueGetter: (value) => value || '—',
      },
      {
        field: 'actions',
        headerName: 'Actions',
        minWidth: 180,
        sortable: false,
        filterable: false,
        renderCell: (params) => (
          <Stack direction="row" spacing={0.5}>
            <Button
              onClick={() => {
                setEditingException(params.row)
                setExceptionFormOpen(true)
              }}
              size="small"
              startIcon={<EditOutlinedIcon />}
            >
              Edit
            </Button>
            <Button
              color="error"
              onClick={() => setExceptionToDelete(params.row)}
              size="small"
              startIcon={<DeleteOutlineIcon />}
            >
              Delete
            </Button>
          </Stack>
        ),
      },
    ],
    [],
  )

  async function confirmDeleteHours() {
    if (!hoursToDelete) {
      return
    }
    try {
      await deleteHoursMutation.mutateAsync({
        staffId,
        hoursId: hoursToDelete.id,
      })
      setHoursToDelete(null)
    } catch {
      // The normalized API error is displayed in the dialog.
    }
  }

  async function confirmDeleteException() {
    if (!exceptionToDelete) {
      return
    }
    try {
      await deleteExceptionMutation.mutateAsync({
        staffId,
        exceptionId: exceptionToDelete.id,
      })
      setExceptionToDelete(null)
    } catch {
      // The normalized API error is displayed in the dialog.
    }
  }

  if (!hasValidStaffId) {
    return <Alert severity="error">Invalid staff ID.</Alert>
  }

  if (staffQuery.isPending) {
    return (
      <Box sx={{ display: 'grid', minHeight: 240, placeItems: 'center' }}>
        <CircularProgress aria-label="Loading staff availability" />
      </Box>
    )
  }

  if (staffQuery.isError) {
    return (
      <Alert
        action={
          <Button color="inherit" onClick={() => staffQuery.refetch()}>
            Retry
          </Button>
        }
        severity="error"
      >
        {staffQuery.error.message}
      </Alert>
    )
  }

  const staff = staffQuery.data

  return (
    <Stack spacing={3}>
      <Box>
        <Button
          component={RouterLink}
          startIcon={<ArrowBackIcon />}
          to="/staff"
        >
          Back to staff
        </Button>
      </Box>

      <div>
        <Typography component="h1" fontWeight={800} variant="h4">
          {staff.firstName} {staff.lastName}
        </Typography>
        <Typography color="text.secondary" mt={0.75}>
          Weekly hours and date-specific exceptions used for bookable slots.
        </Typography>
      </div>

      <Box
        sx={{
          alignItems: { sm: 'center' },
          display: 'flex',
          flexDirection: { xs: 'column', sm: 'row' },
          gap: 2,
          justifyContent: 'space-between',
        }}
      >
        <Typography component="h2" fontWeight={800} variant="h5">
          Weekly hours
        </Typography>
        <Button
          onClick={() => {
            setEditingHours(null)
            setHoursFormOpen(true)
          }}
          startIcon={<AddIcon />}
          variant="contained"
        >
          Add hours
        </Button>
      </Box>
      <Paper
        elevation={0}
        sx={{ border: 1, borderColor: 'divider', borderRadius: 3, p: 2 }}
      >
        {weeklyHoursQuery.isError ? (
          <Alert
            action={
              <Button
                color="inherit"
                onClick={() => weeklyHoursQuery.refetch()}
                size="small"
              >
                Retry
              </Button>
            }
            severity="error"
          >
            {weeklyHoursQuery.error.message}
          </Alert>
        ) : (
          <DataGrid
            autoHeight
            columns={weeklyHoursColumns}
            disableRowSelectionOnClick
            loading={weeklyHoursQuery.isFetching}
            initialState={{
              pagination: { paginationModel: { pageSize: 10 } },
            }}
            pageSizeOptions={[10, 25, 100]}
            rows={weeklyHoursRows}
          />
        )}
      </Paper>

      <Box
        sx={{
          alignItems: { sm: 'center' },
          display: 'flex',
          flexDirection: { xs: 'column', sm: 'row' },
          gap: 2,
          justifyContent: 'space-between',
        }}
      >
        <Typography component="h2" fontWeight={800} variant="h5">
          Exceptions
        </Typography>
        <Button
          onClick={() => {
            setEditingException(null)
            setExceptionFormOpen(true)
          }}
          startIcon={<AddIcon />}
          variant="contained"
        >
          Add exception
        </Button>
      </Box>
      <Paper
        elevation={0}
        sx={{ border: 1, borderColor: 'divider', borderRadius: 3, p: 2 }}
      >
        <Stack
          direction={{ xs: 'column', sm: 'row' }}
          mb={2}
          spacing={2}
        >
          <TextField
            label="From"
            onChange={(event) =>
              setExceptionRange((current) => ({
                ...current,
                from: event.target.value,
              }))
            }
            type="date"
            value={exceptionRange.from}
          />
          <TextField
            label="To"
            onChange={(event) =>
              setExceptionRange((current) => ({
                ...current,
                to: event.target.value,
              }))
            }
            type="date"
            value={exceptionRange.to}
          />
        </Stack>
        {rangeError ? (
          <Alert severity="warning">{rangeError}</Alert>
        ) : exceptionsQuery.isError ? (
          <Alert
            action={
              <Button
                color="inherit"
                onClick={() => exceptionsQuery.refetch()}
                size="small"
              >
                Retry
              </Button>
            }
            severity="error"
          >
            {exceptionsQuery.error.message}
          </Alert>
        ) : (
          <DataGrid
            autoHeight
            columns={exceptionColumns}
            disableRowSelectionOnClick
            loading={exceptionsQuery.isFetching}
            initialState={{
              pagination: { paginationModel: { pageSize: 10 } },
            }}
            pageSizeOptions={[10, 25, 100]}
            rows={exceptionsQuery.data ?? []}
          />
        )}
      </Paper>

      {hoursFormOpen && (
        <WeeklyHoursFormDialog
          hours={editingHours}
          onClose={() => setHoursFormOpen(false)}
          staffId={staffId}
        />
      )}
      {exceptionFormOpen && (
        <AvailabilityExceptionFormDialog
          exception={editingException}
          onClose={() => setExceptionFormOpen(false)}
          staffId={staffId}
        />
      )}
      {hoursToDelete && (
        <DeleteAvailabilityDialog
          description={`${formatDayOfWeek(hoursToDelete.dayOfWeek)} ${formatTime(hoursToDelete.startTime)}–${formatTime(hoursToDelete.endTime)} will be removed.`}
          errorMessage={
            deleteHoursMutation.isError
              ? deleteHoursMutation.error.message
              : undefined
          }
          isPending={deleteHoursMutation.isPending}
          onClose={() => {
            deleteHoursMutation.reset()
            setHoursToDelete(null)
          }}
          onConfirm={confirmDeleteHours}
          title="Delete weekly hours?"
        />
      )}
      {exceptionToDelete && (
        <DeleteAvailabilityDialog
          description={`${formatExceptionType(exceptionToDelete.type)} on ${dateFormatter.format(new Date(`${exceptionToDelete.exceptionDate}T00:00:00`))} will be removed.`}
          errorMessage={
            deleteExceptionMutation.isError
              ? deleteExceptionMutation.error.message
              : undefined
          }
          isPending={deleteExceptionMutation.isPending}
          onClose={() => {
            deleteExceptionMutation.reset()
            setExceptionToDelete(null)
          }}
          onConfirm={confirmDeleteException}
          title="Delete exception?"
        />
      )}
    </Stack>
  )
}

export default StaffAvailabilityPage
