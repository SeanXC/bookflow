import { useMemo, useState } from 'react'
import ArrowBackIcon from '@mui/icons-material/ArrowBack'
import EditOutlinedIcon from '@mui/icons-material/EditOutlined'
import {
  Alert,
  Box,
  Button,
  Chip,
  CircularProgress,
  Divider,
  Link,
  Paper,
  Stack,
  Typography,
} from '@mui/material'
import { DataGrid } from '@mui/x-data-grid'
import { Link as RouterLink, useParams } from 'react-router-dom'

import {
  useCustomer,
  useCustomerAppointments,
} from '../api/customerQueries.js'
import CustomerFormDialog from '../components/CustomerFormDialog.jsx'

const dateTimeFormatter = new Intl.DateTimeFormat('en-IE', {
  dateStyle: 'medium',
  timeStyle: 'short',
})

const statusColors = {
  CONFIRMED: 'primary',
  COMPLETED: 'success',
  CANCELLED: 'default',
}

function CustomerDetailPage() {
  const { customerId: customerIdParam } = useParams()
  const customerId = Number(customerIdParam)
  const hasValidCustomerId = Number.isInteger(customerId) && customerId > 0
  const [paginationModel, setPaginationModel] = useState({
    page: 0,
    pageSize: 10,
  })
  const [formOpen, setFormOpen] = useState(false)
  const customerQuery = useCustomer(customerId, hasValidCustomerId)
  const appointmentsQuery = useCustomerAppointments(
    customerId,
    paginationModel,
    hasValidCustomerId,
  )

  /** @type {import('@mui/x-data-grid').GridColDef<
   *   import('../../appointments/types.js').Appointment>[]}
   */
  const columns = useMemo(
    () => [
      {
        field: 'startTime',
        headerName: 'Date and time',
        minWidth: 190,
        flex: 1,
        valueFormatter: (value) => dateTimeFormatter.format(new Date(value)),
      },
      {
        field: 'service',
        headerName: 'Service',
        minWidth: 180,
        flex: 1,
        valueGetter: (value) => value.name,
      },
      {
        field: 'staff',
        headerName: 'Staff',
        minWidth: 180,
        flex: 1,
        valueGetter: (value) => `${value.firstName} ${value.lastName}`,
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
    ],
    [],
  )

  if (!hasValidCustomerId) {
    return <Alert severity="error">Invalid customer ID.</Alert>
  }

  if (customerQuery.isPending) {
    return (
      <Box sx={{ display: 'grid', minHeight: 240, placeItems: 'center' }}>
        <CircularProgress aria-label="Loading customer" />
      </Box>
    )
  }

  if (customerQuery.isError) {
    return (
      <Alert
        action={
          <Button color="inherit" onClick={() => customerQuery.refetch()}>
            Retry
          </Button>
        }
        severity="error"
      >
        {customerQuery.error.message}
      </Alert>
    )
  }

  const customer = customerQuery.data

  return (
    <Stack spacing={3}>
      <Box>
        <Button
          component={RouterLink}
          startIcon={<ArrowBackIcon />}
          to="/customers"
        >
          Back to customers
        </Button>
      </Box>

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
            {customer.firstName} {customer.lastName}
          </Typography>
          <Typography color="text.secondary" mt={0.75}>
            Customer details and appointment history
          </Typography>
        </div>
        <Button
          onClick={() => setFormOpen(true)}
          startIcon={<EditOutlinedIcon />}
          variant="contained"
        >
          Edit customer
        </Button>
      </Box>

      <Paper
        elevation={0}
        sx={{ border: 1, borderColor: 'divider', borderRadius: 3, p: 3 }}
      >
        <Stack
          direction={{ xs: 'column', md: 'row' }}
          divider={<Divider flexItem orientation="vertical" />}
          spacing={3}
        >
          <Box sx={{ flex: 1 }}>
            <Typography color="text.secondary" variant="body2">
              Email
            </Typography>
            {customer.email ? (
              <Link href={`mailto:${customer.email}`}>{customer.email}</Link>
            ) : (
              <Typography>—</Typography>
            )}
          </Box>
          <Box sx={{ flex: 1 }}>
            <Typography color="text.secondary" variant="body2">
              Phone
            </Typography>
            {customer.phone ? (
              <Link href={`tel:${customer.phone}`}>{customer.phone}</Link>
            ) : (
              <Typography>—</Typography>
            )}
          </Box>
          <Box sx={{ flex: 2 }}>
            <Typography color="text.secondary" variant="body2">
              Notes
            </Typography>
            <Typography sx={{ whiteSpace: 'pre-wrap' }}>
              {customer.notes || '—'}
            </Typography>
          </Box>
        </Stack>
      </Paper>

      <Box>
        <Typography component="h2" fontWeight={800} variant="h5">
          Appointment history
        </Typography>
      </Box>
      <Paper
        elevation={0}
        sx={{ border: 1, borderColor: 'divider', borderRadius: 3, p: 2 }}
      >
        {appointmentsQuery.isError ? (
          <Alert
            action={
              <Button
                color="inherit"
                onClick={() => appointmentsQuery.refetch()}
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
            disableColumnSorting
            disableRowSelectionOnClick
            loading={appointmentsQuery.isFetching}
            onPaginationModelChange={setPaginationModel}
            pageSizeOptions={[10, 20, 50]}
            paginationMode="server"
            paginationModel={paginationModel}
            rowCount={appointmentsQuery.data?.totalElements ?? 0}
            rows={appointmentsQuery.data?.content ?? []}
          />
        )}
      </Paper>

      {formOpen && (
        <CustomerFormDialog
          customer={customer}
          onClose={() => setFormOpen(false)}
        />
      )}
    </Stack>
  )
}

export default CustomerDetailPage
