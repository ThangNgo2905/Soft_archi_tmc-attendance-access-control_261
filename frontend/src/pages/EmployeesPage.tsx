import { ComingSoon } from '../components/common/ComingSoon'
import { PageHeader } from '../components/common/PageHeader'

export function EmployeesPage() {
  return (
    <>
      <PageHeader title="Employees" subtitle="User and QR code management" />
      <ComingSoon area="Employees / QR lifecycle" actors="HR staff, System administrator" />
    </>
  )
}
