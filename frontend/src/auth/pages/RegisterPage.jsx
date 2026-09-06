import { useState } from 'react'
import { Alert, Button, Link, Stack, TextField } from '@mui/material'
import { Link as RouterLink, Navigate, useNavigate } from 'react-router-dom'

import AuthPageLayout from '../components/AuthPageLayout.jsx'
import { useAuth } from '../context/useAuth.js'

function RegisterPage() {
  const { isAuthenticated, register } = useAuth()
  const navigate = useNavigate()
  const [businessName, setBusinessName] = useState('')
  const [businessEmail, setBusinessEmail] = useState('')
  const [businessPhone, setBusinessPhone] = useState('')
  const [ownerEmail, setOwnerEmail] = useState('')
  const [password, setPassword] = useState('')
  const [errorMessage, setErrorMessage] = useState('')
  const [isSubmitting, setIsSubmitting] = useState(false)

  if (isAuthenticated) {
    return <Navigate replace to="/" />
  }

  /**
   * @param {import('react').FormEvent<HTMLFormElement>} event
   */
  async function handleSubmit(event) {
    event.preventDefault()
    setErrorMessage('')
    setIsSubmitting(true)

    try {
      await register({
        businessName: businessName.trim(),
        businessEmail: businessEmail.trim().toLowerCase(),
        ...(businessPhone.trim()
          ? { businessPhone: businessPhone.trim() }
          : {}),
        ownerEmail: ownerEmail.trim().toLowerCase(),
        password,
      })
      navigate('/', { replace: true })
    } catch (error) {
      setErrorMessage(
        error instanceof Error
          ? error.message
          : 'Unable to create the business account.',
      )
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <AuthPageLayout
      footer={
        <>
          Already have an account?{' '}
          <Link component={RouterLink} fontWeight={700} to="/login">
            Sign in
          </Link>
        </>
      }
      subtitle="Create your business workspace and Owner account."
      title="Start using BookFlow"
    >
      <Stack component="form" onSubmit={handleSubmit} spacing={2.5}>
        {errorMessage && <Alert severity="error">{errorMessage}</Alert>}
        <TextField
          autoComplete="organization"
          autoFocus
          fullWidth
          label="Business name"
          onChange={(event) => setBusinessName(event.target.value)}
          required
          slotProps={{ htmlInput: { maxLength: 150 } }}
          value={businessName}
        />
        <TextField
          autoComplete="email"
          fullWidth
          label="Business email"
          onChange={(event) => setBusinessEmail(event.target.value)}
          required
          slotProps={{ htmlInput: { maxLength: 254 } }}
          type="email"
          value={businessEmail}
        />
        <TextField
          autoComplete="tel"
          fullWidth
          label="Business phone (optional)"
          onChange={(event) => setBusinessPhone(event.target.value)}
          slotProps={{ htmlInput: { maxLength: 30 } }}
          type="tel"
          value={businessPhone}
        />
        <TextField
          autoComplete="email"
          fullWidth
          label="Owner email"
          onChange={(event) => setOwnerEmail(event.target.value)}
          required
          slotProps={{ htmlInput: { maxLength: 254 } }}
          type="email"
          value={ownerEmail}
        />
        <TextField
          autoComplete="new-password"
          fullWidth
          helperText="Use between 8 and 72 characters."
          label="Password"
          onChange={(event) => setPassword(event.target.value)}
          required
          slotProps={{ htmlInput: { minLength: 8, maxLength: 72 } }}
          type="password"
          value={password}
        />
        <Button
          disabled={isSubmitting}
          fullWidth
          size="large"
          type="submit"
          variant="contained"
        >
          {isSubmitting ? 'Creating account…' : 'Create account'}
        </Button>
      </Stack>
    </AuthPageLayout>
  )
}

export default RegisterPage
