import { Route, Routes } from 'react-router-dom'
import { AppShell } from './components/layout/AppShell'
import { AttendancePage } from './pages/AttendancePage'
import { DashboardPage } from './pages/DashboardPage'
import { EmployeesPage } from './pages/EmployeesPage'
import { GatesPage } from './pages/GatesPage'
import { MyQrPage } from './pages/MyQrPage'
import { ReportsPage } from './pages/ReportsPage'
import { SettingsPage } from './pages/SettingsPage'

export function AppRoutes() {
  return (
    <Routes>
      <Route element={<AppShell />}>
        <Route index element={<DashboardPage />} />
        <Route path="my-qr" element={<MyQrPage />} />
        <Route path="employees" element={<EmployeesPage />} />
        <Route path="gates" element={<GatesPage />} />
        <Route path="attendance" element={<AttendancePage />} />
        <Route path="reports" element={<ReportsPage />} />
        <Route path="settings" element={<SettingsPage />} />
      </Route>
    </Routes>
  )
}
