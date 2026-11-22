import { createTheme } from '@mui/material/styles'

export const theme = createTheme({
  palette: {
    mode: 'light',
    primary: {
      main: '#0b3d2e',
    },
    secondary: {
      main: '#c45c26',
    },
    background: {
      default: '#f4f6f5',
      paper: '#ffffff',
    },
  },
  typography: {
    fontFamily: '"IBM Plex Sans", "Segoe UI", sans-serif',
    h4: { fontWeight: 700 },
    h5: { fontWeight: 600 },
  },
  shape: {
    borderRadius: 8,
  },
})
