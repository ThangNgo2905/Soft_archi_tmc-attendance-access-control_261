import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import { AppProviders } from './app/providers'
import { KioskPage } from './pages/KioskPage'

createRoot(document.getElementById('root')!).render(<StrictMode><AppProviders><KioskPage /></AppProviders></StrictMode>)
