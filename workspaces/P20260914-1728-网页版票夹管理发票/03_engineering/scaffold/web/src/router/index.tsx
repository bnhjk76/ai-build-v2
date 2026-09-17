import { createBrowserRouter, Navigate } from 'react-router-dom'
import { RequireAuth } from './RequireAuth'
import { AppShell } from '../components/AppShell'
import LoginPage from '../pages/Login'
import RegisterPage from '../pages/Register'
import InvoicesPage from '../pages/Invoices'
import InvoiceFormPage from '../pages/InvoiceForm'
import InvoiceDetailPage from '../pages/InvoiceDetail'

export const router = createBrowserRouter([
  { path: '/login', element: <LoginPage /> },
  { path: '/register', element: <RegisterPage /> },
  {
    path: '/',
    element: <RequireAuth><AppShell /></RequireAuth>,
    children: [
      { index: true, element: <Navigate to="/invoices" replace /> },
      { path: 'invoices', element: <InvoicesPage /> },
      { path: 'invoices/new', element: <InvoiceFormPage /> },
      { path: 'invoices/:id', element: <InvoiceDetailPage /> },
    ],
  },
])
