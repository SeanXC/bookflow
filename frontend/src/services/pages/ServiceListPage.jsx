import { useMemo, useState } from 'react'
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

import { useDebouncedValue } from '../../shared/hooks/useDebouncedValue.js'
import { useServices } from '../api/serviceQueries.js'

const INITIAL_PAGINATION = {
  page: 0,
  pageSize: 20,
}

const INITIAL_SORT = [
  {
    field: 'name',
    sort: 'asc',
  },
]

const currencyFormatter = new Intl.NumberFormat('en-IE', {
  style: 'currency',
  currency: 'EUR',
})

function ServiceListPage() {
  const [searchInput, setSearchInput] = useState('')
  const [activeFilter, setActiveFilter] = useState('active')
  const [paginationModel, setPaginationModel] = useState(INITIAL_PAGINATION)
  const [sortModel, setSortModel] = useState(INITIAL_SORT)
  const search = useDebouncedValue(searchInput.trim())
  const active =
    activeFilter === 'all' ? undefined : activeFilter === 'active'

  const servicesQuery = useServices({
    search,
    active,
    page: paginationModel.page,
    pageSize: paginationModel.pageSize,
    sortModel,
  })

  /** @type {import('@mui/x-data-grid').GridColDef<
   *   import('../types.js').ServiceItem>[]}
   */
  const columns = useMemo(
    () => [
      {
        field: 'name',
        headerName: 'Service',
        minWidth: 180,
        flex: 1,
      },
      {
        field: 'description',
        headerName: 'Description',
        minWidth: 240,
        flex: 1.5,
        sortable: false,
        valueGetter: (value) => value || '—',
      },
      {
        field: 'price',
        headerName: 'Price',
        minWidth: 120,
        valueFormatter: (value) => currencyFormatter.format(Number(value)),
      },
      {
        field: 'durationMinutes',
        headerName: 'Duration',
        minWidth: 130,
        valueFormatter: (value) => `${value} min`,
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
    ],
    [],
  )

  function resetToFirstPage() {
    setPaginationModel((current) => ({ ...current, page: 0 }))
  }

  return (
    <Stack spacing={3}>
      <Box>
        <Typography component="h1" fontWeight={800} variant="h4">
          Services
        </Typography>
        <Typography color="text.secondary" mt={0.75}>
          Browse services, pricing, duration, and availability.
        </Typography>
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
            fullWidth
            label="Search services"
            onChange={(event) => {
              setSearchInput(event.target.value)
              resetToFirstPage()
            }}
            placeholder="Name or description"
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
            <InputLabel id="service-status-label">Status</InputLabel>
            <Select
              label="Status"
              labelId="service-status-label"
              onChange={(event) => {
                setActiveFilter(event.target.value)
                resetToFirstPage()
              }}
              value={activeFilter}
            >
              <MenuItem value="all">All services</MenuItem>
              <MenuItem value="active">Active</MenuItem>
              <MenuItem value="inactive">Inactive</MenuItem>
            </Select>
          </FormControl>
        </Stack>

        {servicesQuery.isError ? (
          <Alert
            action={
              <Button
                color="inherit"
                onClick={() => servicesQuery.refetch()}
                size="small"
              >
                Retry
              </Button>
            }
            severity="error"
          >
            {servicesQuery.error.message}
          </Alert>
        ) : (
          <DataGrid
            autoHeight
            columns={columns}
            disableRowSelectionOnClick
            loading={servicesQuery.isFetching}
            onPaginationModelChange={setPaginationModel}
            onSortModelChange={(nextSortModel) => {
              setSortModel(nextSortModel)
              resetToFirstPage()
            }}
            pageSizeOptions={[10, 20, 50, 100]}
            paginationMode="server"
            paginationModel={paginationModel}
            rowCount={servicesQuery.data?.totalElements ?? 0}
            rows={servicesQuery.data?.content ?? []}
            sortingMode="server"
            sortModel={sortModel}
          />
        )}
      </Paper>
    </Stack>
  )
}

export default ServiceListPage
