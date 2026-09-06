import { useMemo, useState } from 'react'
import AddIcon from '@mui/icons-material/Add'
import EditOutlinedIcon from '@mui/icons-material/EditOutlined'
import SearchIcon from '@mui/icons-material/Search'
import VisibilityOutlinedIcon from '@mui/icons-material/VisibilityOutlined'
import {
  Alert,
  Box,
  Button,
  InputAdornment,
  Link,
  Paper,
  Stack,
  TextField,
  Typography,
} from '@mui/material'
import { DataGrid } from '@mui/x-data-grid'
import { useNavigate } from 'react-router-dom'

import { useDebouncedValue } from '../../shared/hooks/useDebouncedValue.js'
import { useCustomers } from '../api/customerQueries.js'
import CustomerFormDialog from '../components/CustomerFormDialog.jsx'

const INITIAL_PAGINATION = {
  page: 0,
  pageSize: 20,
}

const INITIAL_SORT = [
  {
    field: 'lastName',
    sort: 'asc',
  },
]

const dateFormatter = new Intl.DateTimeFormat('en-IE', {
  dateStyle: 'medium',
})

function CustomerListPage() {
  const navigate = useNavigate()
  const [searchInput, setSearchInput] = useState('')
  const [paginationModel, setPaginationModel] = useState(INITIAL_PAGINATION)
  const [sortModel, setSortModel] = useState(INITIAL_SORT)
  const [formOpen, setFormOpen] = useState(false)
  const [editingCustomer, setEditingCustomer] = useState(
    /** @type {import('../types.js').Customer | null} */ (null),
  )
  const search = useDebouncedValue(searchInput.trim())

  const customersQuery = useCustomers({
    search,
    page: paginationModel.page,
    pageSize: paginationModel.pageSize,
    sortModel,
  })

  /** @type {import('@mui/x-data-grid').GridColDef<
   *   import('../types.js').Customer>[]}
   */
  const columns = useMemo(
    () => [
      {
        field: 'lastName',
        headerName: 'Name',
        minWidth: 200,
        flex: 1,
        valueGetter: (_value, row) => `${row.firstName} ${row.lastName}`,
      },
      {
        field: 'email',
        headerName: 'Email',
        minWidth: 230,
        flex: 1.2,
        sortable: false,
        renderCell: (params) =>
          params.value ? (
            <Link href={`mailto:${params.value}`}>{params.value}</Link>
          ) : (
            '—'
          ),
      },
      {
        field: 'phone',
        headerName: 'Phone',
        minWidth: 160,
        flex: 0.8,
        sortable: false,
        valueGetter: (value) => value || '—',
      },
      {
        field: 'createdAt',
        headerName: 'Customer since',
        minWidth: 160,
        valueFormatter: (value) => dateFormatter.format(new Date(value)),
      },
      {
        field: 'actions',
        headerName: 'Actions',
        minWidth: 170,
        sortable: false,
        filterable: false,
        renderCell: (params) => (
          <Stack direction="row" spacing={0.5}>
            <Button
              onClick={() => navigate(`/customers/${params.row.id}`)}
              size="small"
              startIcon={<VisibilityOutlinedIcon />}
            >
              View
            </Button>
            <Button
              onClick={() => {
                setEditingCustomer(params.row)
                setFormOpen(true)
              }}
              size="small"
              startIcon={<EditOutlinedIcon />}
            >
              Edit
            </Button>
          </Stack>
        ),
      },
    ],
    [navigate],
  )

  function resetToFirstPage() {
    setPaginationModel((current) => ({ ...current, page: 0 }))
  }

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
            Customers
          </Typography>
          <Typography color="text.secondary" mt={0.75}>
            Search customer contact details and appointment records.
          </Typography>
        </div>
        <Button
          onClick={() => {
            setEditingCustomer(null)
            setFormOpen(true)
          }}
          startIcon={<AddIcon />}
          variant="contained"
        >
          Add customer
        </Button>
      </Box>

      <Paper
        elevation={0}
        sx={{ border: 1, borderColor: 'divider', borderRadius: 3, p: 2 }}
      >
        <TextField
          fullWidth
          label="Search customers"
          onChange={(event) => {
            setSearchInput(event.target.value)
            resetToFirstPage()
          }}
          placeholder="Name, email, or phone"
          slotProps={{
            input: {
              startAdornment: (
                <InputAdornment position="start">
                  <SearchIcon />
                </InputAdornment>
              ),
            },
          }}
          sx={{ mb: 2 }}
          value={searchInput}
        />

        {customersQuery.isError ? (
          <Alert
            action={
              <Button
                color="inherit"
                onClick={() => customersQuery.refetch()}
                size="small"
              >
                Retry
              </Button>
            }
            severity="error"
          >
            {customersQuery.error.message}
          </Alert>
        ) : (
          <DataGrid
            autoHeight
            columns={columns}
            disableRowSelectionOnClick
            loading={customersQuery.isFetching}
            onPaginationModelChange={setPaginationModel}
            onSortModelChange={(nextSortModel) => {
              setSortModel(nextSortModel)
              resetToFirstPage()
            }}
            pageSizeOptions={[10, 20, 50, 100]}
            paginationMode="server"
            paginationModel={paginationModel}
            rowCount={customersQuery.data?.totalElements ?? 0}
            rows={customersQuery.data?.content ?? []}
            sortingMode="server"
            sortModel={sortModel}
          />
        )}
      </Paper>

      {formOpen && (
        <CustomerFormDialog
          customer={editingCustomer}
          onClose={() => setFormOpen(false)}
        />
      )}
    </Stack>
  )
}

export default CustomerListPage
