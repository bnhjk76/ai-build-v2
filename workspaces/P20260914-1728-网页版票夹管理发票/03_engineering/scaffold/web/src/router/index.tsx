import { createBrowserRouter, Navigate } from 'react-router-dom'
import { RequireAuth } from './RequireAuth'
import LoginPage from '../pages/Login'
import RegisterPage from '../pages/Register'
import InvoicesPage from '../pages/Invoices'

export const router = createBrowserRouter([
  { path: '/login', element: <LoginPage /> },
  { path: '/register', element: <RegisterPage /> },
  {
    path: '/',
    element: <RequireAuth><InvoicesPage /></RequireAuth>,
    children: [
      { path: 'invoices', element: <InvoicesPage /> },
      { index: true, element: <Navigate to="/invoices" replace /> },
    ],
  },
  { path: '*', element: <Navigate to="/invoices" replace /> },
])
