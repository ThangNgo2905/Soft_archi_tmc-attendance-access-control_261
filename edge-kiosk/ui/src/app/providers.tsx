import { CssBaseline, ThemeProvider, createTheme } from '@mui/material'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { useState, type ReactNode } from 'react'
import { BrowserRouter } from 'react-router-dom'

const theme = createTheme({
  palette: { primary: { main: '#0b3d2e' }, secondary: { main: '#c45c26' }, background: { default: '#f4f6f5' } },
  typography: { fontFamily: '"Segoe UI", sans-serif', h4: { fontWeight: 700 } },
  shape: { borderRadius: 12 },
})

export function AppProviders({ children }: { children: ReactNode }) {
  const [queryClient] = useState(() => new QueryClient({ defaultOptions: { queries: { retry: false } } }))
  return <QueryClientProvider client={queryClient}><ThemeProvider theme={theme}><CssBaseline /><BrowserRouter>{children}</BrowserRouter></ThemeProvider></QueryClientProvider>
}
