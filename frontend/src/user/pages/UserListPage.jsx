import { useMemo, useState } from 'react'
import AddIcon from '@mui/icons-material/Add'
import {
  Alert,
  Box,
  Button,
  Chip,
  Paper,
  Stack,
  Typography,
} from '@mui/material'
import { DataGrid } from '@mui/x-data-grid'

import { useUsers } from '../api/userQueries.js'
import UserEnabledDialog from '../components/UserEnabledDialog.jsx'
import UserFormDialog from '../components/UserFormDialog.jsx'

const INITIAL_PAGINATION = {
  page: 0,
  pageSize: 20,
}

const INITIAL_SORT = [
  {
    field: 'email',
    sort: 'asc',
  },
]

const dateFormatter = new Intl.DateTimeFormat('en-IE', {
  dateStyle: 'medium',
})

function UserListPage() {
  const [paginationModel, setPaginationModel] = useState(INITIAL_PAGINATION)
  const [sortModel, setSortModel] = useState(INITIAL_SORT)
  const [formOpen, setFormOpen] = useState(false)
  const [userToUpdate, setUserToUpdate] = useState(
    /** @type {import('../types.js').ManagedUser | null} */ (null),
  )
  const usersQuery = useUsers({
    page: paginationModel.page,
    pageSize: paginationModel.pageSize,
    sortModel,
  })

  /** @type {import('@mui/x-data-grid').GridColDef<
   *   import('../types.js').ManagedUser>[]}
   */
  const columns = useMemo(
    () => [
      {
        field: 'email',
        headerName: 'Email',
        minWidth: 260,
        flex: 1.4,
      },
      {
        field: 'role',
        headerName: 'Role',
        minWidth: 160,
        flex: 0.7,
        renderCell: (params) => (
          <Chip label={params.value} size="small" variant="outlined" />
        ),
      },
      {
        field: 'enabled',
        headerName: 'Status',
        minWidth: 130,
        flex: 0.6,
        renderCell: (params) => (
          <Chip
            color={params.value ? 'success' : 'default'}
            label={params.value ? 'Enabled' : 'Disabled'}
            size="small"
          />
        ),
      },
      {
        field: 'createdAt',
        headerName: 'Created',
        minWidth: 170,
        flex: 0.8,
        valueFormatter: (value) => dateFormatter.format(new Date(value)),
      },
      {
        field: 'actions',
        headerName: 'Actions',
        minWidth: 120,
        sortable: false,
        filterable: false,
        renderCell: (params) =>
          params.row.role === 'OWNER' ? null : (
            <Button
              color={params.row.enabled ? 'error' : 'primary'}
              onClick={() => setUserToUpdate(params.row)}
              size="small"
            >
              {params.row.enabled ? 'Disable' : 'Enable'}
            </Button>
          ),
      },
    ],
    [],
  )

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
            Users
          </Typography>
          <Typography color="text.secondary" mt={0.75}>
            Manage Receptionist and Staff login accounts.
          </Typography>
        </div>
        <Button
          onClick={() => setFormOpen(true)}
          startIcon={<AddIcon />}
          variant="contained"
        >
          Create account
        </Button>
      </Box>

      <Paper
        elevation={0}
        sx={{ border: 1, borderColor: 'divider', borderRadius: 3, p: 2 }}
      >
        {usersQuery.isError ? (
          <Alert
            action={
              <Button
                color="inherit"
                onClick={() => usersQuery.refetch()}
                size="small"
              >
                Retry
              </Button>
            }
            severity="error"
          >
            {usersQuery.error.message}
          </Alert>
        ) : (
          <DataGrid
            autoHeight
            columns={columns}
            disableRowSelectionOnClick
            loading={usersQuery.isFetching}
            onPaginationModelChange={setPaginationModel}
            onSortModelChange={(nextSortModel) => {
              setSortModel(nextSortModel)
              setPaginationModel((current) => ({ ...current, page: 0 }))
            }}
            pageSizeOptions={[10, 20, 50, 100]}
            paginationMode="server"
            paginationModel={paginationModel}
            rowCount={usersQuery.data?.totalElements ?? 0}
            rows={usersQuery.data?.content ?? []}
            sortingMode="server"
            sortModel={sortModel}
          />
        )}
      </Paper>

      {formOpen && <UserFormDialog onClose={() => setFormOpen(false)} />}
      {userToUpdate && (
        <UserEnabledDialog
          onClose={() => setUserToUpdate(null)}
          user={userToUpdate}
        />
      )}
    </Stack>
  )
}

export default UserListPage
