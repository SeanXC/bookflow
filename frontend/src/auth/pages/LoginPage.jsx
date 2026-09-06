import { useState } from 'react'
import {
  Alert,
  Button,
  Link,
  Stack,
  TextField,
} from '@mui/material'
import { Link as RouterLink, Navigate, useNavigate } from 'react-router-dom'

import AuthPageLayout from '../components/AuthPageLayout.jsx'
import { useAuth } from '../context/useAuth.js'

function LoginPage() {
  const { isAuthenticated, login } = useAuth()
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
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
      await login({
        email: email.trim().toLowerCase(),
        password,
      })
      navigate('/', { replace: true })
    } catch (error) {
      setErrorMessage(
        error instanceof Error ? error.message : 'Unable to sign in.',
      )
    } finally {
      setIsSubmitting(false)
    }
  }

  return (
    <AuthPageLayout
      footer={
        <>
          New to BookFlow?{' '}
          <Link component={RouterLink} fontWeight={700} to="/register">
            Create a business account
          </Link>
        </>
      }
      subtitle="Use your business account to continue."
      title="Welcome back"
    >
      <Stack component="form" onSubmit={handleSubmit} spacing={2.5}>
        {errorMessage && <Alert severity="error">{errorMessage}</Alert>}
        <TextField
          autoComplete="email"
          autoFocus
          fullWidth
          label="Email address"
          name="email"
          onChange={(event) => setEmail(event.target.value)}
          required
          slotProps={{ htmlInput: { maxLength: 254 } }}
          type="email"
          value={email}
        />
        <TextField
          autoComplete="current-password"
          fullWidth
          label="Password"
          name="password"
          onChange={(event) => setPassword(event.target.value)}
          required
          slotProps={{ htmlInput: { maxLength: 72 } }}
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
          {isSubmitting ? 'Signing in…' : 'Sign in'}
        </Button>
      </Stack>
    </AuthPageLayout>
  )
}

export default LoginPage
