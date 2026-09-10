import { useState } from 'react'
import ArrowBackIcon from '@mui/icons-material/ArrowBack'
import {
  Alert,
  Box,
  Button,
  CircularProgress,
  Paper,
  Stack,
  TextField,
  Typography,
} from '@mui/material'
import PropTypes from 'prop-types'
import { Link as RouterLink, useNavigate, useParams } from 'react-router-dom'

import { ApiError } from '../../api/apiError.js'
import {
  useChatPublicAssistant,
  useConfirmPublicAssistantBooking,
} from '../api/assistantMutations.js'
import { usePublicBusiness } from '../api/publicBookingQueries.js'
import { toPublicAppointment } from '../assistantBooking.js'
import { getAssistantErrorMessage } from '../assistantError.js'
import PublicUnavailableState from '../components/PublicUnavailableState.jsx'
import {
  ASSISTANT_MAX_MESSAGE_LENGTH,
  ASSISTANT_MAX_MESSAGES,
} from '../constants.js'
import { formatSlotRange } from '../format.js'

const currencyFormatter = new Intl.NumberFormat('en-IE', {
  style: 'currency',
  currency: 'EUR',
})

function PublicAssistantPage() {
  const { slug } = useParams()
  const navigate = useNavigate()
  const businessQuery = usePublicBusiness(slug)
  const chatMutation = useChatPublicAssistant(slug ?? '')
  const confirmMutation = useConfirmPublicAssistantBooking(slug ?? '')
  const [draft, setDraft] = useState('')
  const [messages, setMessages] = useState(
    /** @type {import('../types.js').AssistantChatMessage[]} */ ([]),
  )
  const [proposal, setProposal] = useState(
    /** @type {import('../types.js').AssistantProposal | null} */ (null),
  )

  if (businessQuery.isPending) {
    return (
      <Box sx={{ display: 'grid', minHeight: 240, placeItems: 'center' }}>
        <CircularProgress aria-label="Loading booking assistant" />
      </Box>
    )
  }

  if (
    businessQuery.error instanceof ApiError &&
    businessQuery.error.status === 404
  ) {
    return (
      <PublicUnavailableState
        onRetry={() => {
          businessQuery.refetch()
        }}
      />
    )
  }

  if (businessQuery.isError) {
    return (
      <Alert
        action={
          <Button color="inherit" onClick={() => businessQuery.refetch()}>
            Retry
          </Button>
        }
        severity="error"
      >
        {businessQuery.error?.message ?? 'Unable to load this booking page.'}
      </Alert>
    )
  }

  const busy = chatMutation.isPending || confirmMutation.isPending
  const chatError = chatMutation.isError
    ? getAssistantErrorMessage(chatMutation.error)
    : null
  const confirmError = confirmMutation.isError
    ? getAssistantErrorMessage(confirmMutation.error)
    : null

  /**
   * @param {string} content
   */
  async function handleSend(content) {
    const trimmed = content.trim()
    if (!trimmed || busy || !slug) {
      return
    }
    const history = [
      ...messages,
      /** @type {import('../types.js').AssistantChatMessage} */ ({
        role: 'USER',
        content: trimmed.slice(0, ASSISTANT_MAX_MESSAGE_LENGTH),
      }),
    ].slice(-ASSISTANT_MAX_MESSAGES)
    setDraft('')
    setMessages(history)
    chatMutation.reset()
    confirmMutation.reset()
    try {
      const response = await chatMutation.mutateAsync(history)
      setMessages([
        ...history,
        { role: 'ASSISTANT', content: response.message },
      ])
      setProposal(response.proposal ?? null)
    } catch {
      // Surface the mutation error in the form below.
    }
  }

  async function handleConfirm() {
    if (!proposal?.proposalId || busy || !slug) {
      return
    }
    confirmMutation.reset()
    try {
      const booking = await confirmMutation.mutateAsync(proposal.proposalId)
      navigate(`/book/${slug}/confirmation`, {
        replace: true,
        state: { appointment: toPublicAppointment(booking, proposal) },
      })
    } catch {
      // Surface the mutation error in the form below.
    }
  }

  return (
    <Stack spacing={3}>
      <Button
        component={RouterLink}
        startIcon={<ArrowBackIcon />}
        sx={{ alignSelf: 'flex-start' }}
        to={`/book/${slug}`}
      >
        Back to services
      </Button>

      <Stack spacing={1.25}>
        <Typography component="h1" fontWeight={800} variant="h4">
          Book with the assistant
        </Typography>
        <Typography color="text.secondary">
          Tell me what you need. I only use real availability for{' '}
          {businessQuery.data?.name}, and I will wait for you to confirm before
          creating an appointment.
        </Typography>
      </Stack>

      <Paper
        elevation={0}
        sx={{
          border: 1,
          borderColor: 'divider',
          borderRadius: 3,
          display: 'grid',
          minHeight: 320,
          p: 2.5,
        }}
      >
        <Stack spacing={2} sx={{ minHeight: 280 }}>
          {messages.length === 0 ? (
            <Typography color="text.secondary">
              Try “Haircut with Anna on Monday morning”.
            </Typography>
          ) : (
            <Stack spacing={1.5} sx={{ flex: 1 }}>
              {messages.map((message, index) => (
                <ChatBubble key={`${message.role}-${index}`} message={message} />
              ))}
            </Stack>
          )}
          {chatMutation.isPending ? (
            <Box sx={{ display: 'grid', placeItems: 'center', py: 1 }}>
              <CircularProgress
                aria-label="Assistant is thinking"
                size={28}
              />
            </Box>
          ) : null}
        </Stack>
      </Paper>

      {proposal ? (
        <ProposalCard
          busy={busy}
          onConfirm={() => {
            void handleConfirm()
          }}
          proposal={proposal}
        />
      ) : null}

      {chatError ? <Alert severity="error">{chatError}</Alert> : null}
      {confirmError ? <Alert severity="error">{confirmError}</Alert> : null}

      <Box
        component="form"
        onSubmit={(event) => {
          event.preventDefault()
          void handleSend(draft)
        }}
      >
        <Stack direction={{ xs: 'column', sm: 'row' }} spacing={1.5}>
          <TextField
            disabled={busy}
            fullWidth
            inputProps={{ maxLength: ASSISTANT_MAX_MESSAGE_LENGTH }}
            label="Message"
            minRows={2}
            multiline
            onChange={(event) => setDraft(event.target.value)}
            onKeyDown={(event) => {
              if (event.key === 'Enter' && !event.shiftKey) {
                event.preventDefault()
                void handleSend(draft)
              }
            }}
            value={draft}
          />
          <Button
            disabled={busy || !draft.trim()}
            sx={{ alignSelf: { sm: 'flex-end' }, minWidth: 120 }}
            type="submit"
            variant="contained"
          >
            Send
          </Button>
        </Stack>
      </Box>
    </Stack>
  )
}

function ChatBubble({ message }) {
  const fromGuest = message.role === 'USER'
  return (
    <Box
      sx={{
        alignSelf: fromGuest ? 'flex-end' : 'flex-start',
        bgcolor: fromGuest ? 'primary.main' : 'action.hover',
        borderRadius: 2,
        color: fromGuest ? 'primary.contrastText' : 'text.primary',
        maxWidth: '85%',
        px: 1.75,
        py: 1.25,
      }}
    >
      <Typography sx={{ whiteSpace: 'pre-wrap' }}>{message.content}</Typography>
    </Box>
  )
}

ChatBubble.propTypes = {
  message: PropTypes.shape({
    content: PropTypes.string.isRequired,
    role: PropTypes.oneOf(['USER', 'ASSISTANT']).isRequired,
  }).isRequired,
}

function ProposalCard({ proposal, busy, onConfirm }) {
  const staffName = [proposal.staffFirstName, proposal.staffLastName]
    .filter(Boolean)
    .join(' ')
  const guestName = `${proposal.firstName} ${proposal.lastName}`

  return (
    <Paper
      elevation={0}
      sx={{ border: 1, borderColor: 'primary.main', borderRadius: 3, p: 3 }}
    >
      <Stack spacing={2}>
        <Stack spacing={0.75}>
          <Typography fontWeight={800} variant="h6">
            Review this booking
          </Typography>
          <Typography color="text.secondary">
            Nothing is booked until you confirm.
          </Typography>
        </Stack>
        <Typography>
          {proposal.serviceName ?? 'Service'}
          {staffName ? ` with ${staffName}` : ''}
        </Typography>
        <Typography color="text.secondary">
          {formatSlotRange(proposal.startTime, proposal.endTime)}
        </Typography>
        {proposal.price != null ? (
          <Typography color="text.secondary">
            {currencyFormatter.format(Number(proposal.price))}
            {proposal.durationMinutes
              ? ` · ${proposal.durationMinutes} min`
              : ''}
          </Typography>
        ) : null}
        <Typography color="text.secondary">
          {guestName} · {proposal.email}
        </Typography>
        <Button
          disabled={busy || !proposal.proposalId}
          onClick={onConfirm}
          sx={{ alignSelf: 'flex-start' }}
          variant="contained"
        >
          Confirm booking
        </Button>
      </Stack>
    </Paper>
  )
}

ProposalCard.propTypes = {
  busy: PropTypes.bool.isRequired,
  onConfirm: PropTypes.func.isRequired,
  proposal: PropTypes.shape({
    durationMinutes: PropTypes.number,
    email: PropTypes.string.isRequired,
    endTime: PropTypes.string.isRequired,
    firstName: PropTypes.string.isRequired,
    lastName: PropTypes.string.isRequired,
    price: PropTypes.number,
    proposalId: PropTypes.string,
    serviceName: PropTypes.string,
    staffFirstName: PropTypes.string,
    staffLastName: PropTypes.string,
    startTime: PropTypes.string.isRequired,
  }).isRequired,
}

export default PublicAssistantPage
