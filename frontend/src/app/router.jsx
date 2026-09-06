import { createBrowserRouter } from 'react-router-dom'

import LoginPage from '../auth/pages/LoginPage.jsx'
import RegisterPage from '../auth/pages/RegisterPage.jsx'
import App from './App.jsx'

export const router = createBrowserRouter([
  {
    path: '/',
    element: <App />,
  },
  {
    path: '/login',
    element: <LoginPage />,
  },
  {
    path: '/register',
    element: <RegisterPage />,
  },
])
