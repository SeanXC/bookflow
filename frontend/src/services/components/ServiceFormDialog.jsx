import { useState } from 'react'
import {
  Alert,
  Button,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  InputAdornment,
  Stack,
  TextField,
} from '@mui/material'
import PropTypes from 'prop-types'

import {
  useCreateService,
  useUpdateService,
} from '../api/serviceMutations.js'

function ServiceFormDialog({ onClose, service }) {
  const createMutation = useCreateService()
  const updateMutation = useUpdateService()
  const [name, setName] = useState(service?.name ?? '')
  const [description, setDescription] = useState(service?.description ?? '')
  const [price, setPrice] = useState(
    service ? String(service.price) : '',
  )
  const [durationMinutes, setDurationMinutes] = useState(
    service ? String(service.durationMinutes) : '',
  )
  const [errorMessage, setErrorMessage] = useState('')
  const isEditing = service !== null
  const isSubmitting = createMutation.isPending || updateMutation.isPending

  /**
   * @param {import('react').FormEvent<HTMLFormElement>} event
   */
  async function handleSubmit(event) {
    event.preventDefault()
    setErrorMessage('')

    const request = {
      name: name.trim(),
      description: description.trim() || null,
      price: Number(price),
      durationMinutes: Number(durationMinutes),
    }

    try {
      if (service) {
        await updateMutation.mutateAsync({ serviceId: service.id, request })
      } else {
        await createMutation.mutateAsync(request)
      }
      onClose()
    } catch (error) {
      setErrorMessage(
        error instanceof Error ? error.message : 'Unable to save service.',
      )
    }
  }

  return (
    <Dialog
      fullWidth
      maxWidth="sm"
      onClose={() => {
        if (!isSubmitting) {
          onClose()
        }
      }}
      open
    >
      <Stack component="form" onSubmit={handleSubmit}>
        <DialogTitle>
          {isEditing ? 'Edit service' : 'Create service'}
        </DialogTitle>
        <DialogContent>
          <Stack spacing={2.5} sx={{ pt: 1 }}>
            {errorMessage && <Alert severity="error">{errorMessage}</Alert>}
            <TextField
              autoFocus
              fullWidth
              label="Service name"
              onChange={(event) => setName(event.target.value)}
              required
              slotProps={{ htmlInput: { maxLength: 150 } }}
              value={name}
            />
            <TextField
              fullWidth
              label="Description (optional)"
              minRows={3}
              multiline
              onChange={(event) => setDescription(event.target.value)}
              slotProps={{ htmlInput: { maxLength: 2000 } }}
              value={description}
            />
            <Stack direction={{ xs: 'column', sm: 'row' }} spacing={2}>
              <TextField
                fullWidth
                label="Price"
                onChange={(event) => setPrice(event.target.value)}
                required
                slotProps={{
                  htmlInput: { min: 0, step: 0.01 },
                  input: {
                    startAdornment: (
                      <InputAdornment position="start">€</InputAdornment>
                    ),
                  },
                }}
                type="number"
                value={price}
              />
              <TextField
                fullWidth
                label="Duration (minutes)"
                onChange={(event) => setDurationMinutes(event.target.value)}
                required
                slotProps={{ htmlInput: { min: 1, step: 1 } }}
                type="number"
                value={durationMinutes}
              />
            </Stack>
          </Stack>
        </DialogContent>
        <DialogActions sx={{ px: 3, pb: 3 }}>
          <Button disabled={isSubmitting} onClick={onClose}>
            Cancel
          </Button>
          <Button disabled={isSubmitting} type="submit" variant="contained">
            {isSubmitting ? 'Saving…' : 'Save'}
          </Button>
        </DialogActions>
      </Stack>
    </Dialog>
  )
}

ServiceFormDialog.propTypes = {
  onClose: PropTypes.func.isRequired,
  service: PropTypes.shape({
    id: PropTypes.number.isRequired,
    name: PropTypes.string.isRequired,
    description: PropTypes.string,
    price: PropTypes.number.isRequired,
    durationMinutes: PropTypes.number.isRequired,
    active: PropTypes.bool.isRequired,
  }),
}

export default ServiceFormDialog
