import { useMemo, useState } from 'react'
import AddIcon from '@mui/icons-material/Add'
import DeleteOutlineIcon from '@mui/icons-material/DeleteOutline'
import EditOutlinedIcon from '@mui/icons-material/EditOutlined'
import SearchIcon from '@mui/icons-material/Search'
import {
  Alert,
  Box,
  Button,
  Chip,
  FormControl,
  InputAdornment,
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
import { useDebouncedValue } from '../../shared/hooks/useDebouncedValue.js'
import { useManagedUsers } from '../../user/api/userQueries.js'
import { useStaff } from '../api/staffQueries.js'
import DeactivateStaffDialog from '../components/DeactivateStaffDialog.jsx'
import StaffFormDialog from '../components/StaffFormDialog.jsx'

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

function StaffListPage() {
  const { user } = useAuth()
  const [searchInput, setSearchInput] = useState('')
  const [activeFilter, setActiveFilter] = useState('active')
  const [paginationModel, setPaginationModel] = useState(INITIAL_PAGINATION)
  const [sortModel, setSortModel] = useState(INITIAL_SORT)
  const [formOpen, setFormOpen] = useState(false)
  const [editingStaff, setEditingStaff] = useState(
    /** @type {import('../types.js').StaffMember | null} */ (null),
  )
  const [staffToDeactivate, setStaffToDeactivate] = useState(
    /** @type {import('../types.js').StaffMember | null} */ (null),
  )
  const search = useDebouncedValue(searchInput.trim())
  const active =
    activeFilter === 'all' ? undefined : activeFilter === 'active'
  const isOwner = user?.role === 'OWNER'
  const usersQuery = useManagedUsers(isOwner)

  const staffQuery = useStaff({
    search,
    active,
    page: paginationModel.page,
    pageSize: paginationModel.pageSize,
    sortModel,
  })

  /** @type {import('@mui/x-data-grid').GridColDef<
   *   import('../types.js').StaffMember>[]}
   */
  const columns = useMemo(() => {
    const baseColumns = [
      {
        field: 'lastName',
        headerName: 'Name',
        minWidth: 200,
        flex: 1,
        valueGetter: (_value, row) => `${row.firstName} ${row.lastName}`,
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
        field: 'userId',
        headerName: 'Login account',
        minWidth: 150,
        sortable: false,
        renderCell: (params) => (
          <Chip
            color={params.value ? 'primary' : 'default'}
            label={params.value ? 'Linked' : 'Not linked'}
            size="small"
            variant="outlined"
          />
        ),
      },
      {
        field: 'active',
        headerName: 'Status',
        minWidth: 120,
        renderCell: (params) => (
          <Chip
            color={params.value ? 'success' : 'default'}
            label={params.value ? 'Active' : 'Inactive'}
            size="small"
          />
        ),
      },
    ]

    if (isOwner) {
      baseColumns.push({
        field: 'actions',
        headerName: 'Actions',
        minWidth: 180,
        sortable: false,
        filterable: false,
        renderCell: (params) => (
          <Stack direction="row" spacing={0.5}>
            <Button
              onClick={() => {
                setEditingStaff(params.row)
                setFormOpen(true)
              }}
              size="small"
              startIcon={<EditOutlinedIcon />}
            >
              Edit
            </Button>
            {params.row.active && (
              <Button
                color="error"
                onClick={() => setStaffToDeactivate(params.row)}
                size="small"
                startIcon={<DeleteOutlineIcon />}
              >
                Deactivate
              </Button>
            )}
          </Stack>
        ),
      })
    }

    return baseColumns
  }, [isOwner])

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
            Staff
          </Typography>
          <Typography color="text.secondary" mt={0.75}>
            Browse staff profiles and their linked login accounts.
          </Typography>
        </div>
        {isOwner && (
          <Button
            onClick={() => {
              setEditingStaff(null)
              setFormOpen(true)
            }}
            startIcon={<AddIcon />}
            variant="contained"
          >
            Add staff
          </Button>
        )}
      </Box>

      {usersQuery.isError && isOwner && (
        <Alert severity="warning">
          Login accounts could not be loaded. Staff can still be managed
          without changing account links.
        </Alert>
      )}

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
            fullWidth
            label="Search staff"
            onChange={(event) => {
              setSearchInput(event.target.value)
              resetToFirstPage()
            }}
            placeholder="First or last name"
            slotProps={{
              input: {
                startAdornment: (
                  <InputAdornment position="start">
                    <SearchIcon />
                  </InputAdornment>
                ),
              },
            }}
            value={searchInput}
          />
          <FormControl sx={{ minWidth: 170 }}>
            <InputLabel id="staff-status-label">Status</InputLabel>
            <Select
              label="Status"
              labelId="staff-status-label"
              onChange={(event) => {
                setActiveFilter(event.target.value)
                resetToFirstPage()
              }}
              value={activeFilter}
            >
              <MenuItem value="all">All staff</MenuItem>
              <MenuItem value="active">Active</MenuItem>
              <MenuItem value="inactive">Inactive</MenuItem>
            </Select>
          </FormControl>
        </Stack>

        {staffQuery.isError ? (
          <Alert
            action={
              <Button
                color="inherit"
                onClick={() => staffQuery.refetch()}
                size="small"
              >
                Retry
              </Button>
            }
            severity="error"
          >
            {staffQuery.error.message}
          </Alert>
        ) : (
          <DataGrid
            autoHeight
            columns={columns}
            disableRowSelectionOnClick
            loading={staffQuery.isFetching}
            onPaginationModelChange={setPaginationModel}
            onSortModelChange={(nextSortModel) => {
              setSortModel(nextSortModel)
              resetToFirstPage()
            }}
            pageSizeOptions={[10, 20, 50, 100]}
            paginationMode="server"
            paginationModel={paginationModel}
            rowCount={staffQuery.data?.totalElements ?? 0}
            rows={staffQuery.data?.content ?? []}
            sortingMode="server"
            sortModel={sortModel}
          />
        )}
      </Paper>

      {isOwner && (
        <>
          {formOpen && (
            <StaffFormDialog
              onClose={() => setFormOpen(false)}
              open
              staff={editingStaff}
              users={usersQuery.data?.content ?? []}
            />
          )}
          {staffToDeactivate && (
            <DeactivateStaffDialog
              onClose={() => setStaffToDeactivate(null)}
              staff={staffToDeactivate}
            />
          )}
        </>
      )}
    </Stack>
  )
}

export default StaffListPage
