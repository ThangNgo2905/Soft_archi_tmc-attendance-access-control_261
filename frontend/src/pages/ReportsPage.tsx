import { ComingSoon } from '../components/common/ComingSoon'
import { PageHeader } from '../components/common/PageHeader'

export function ReportsPage() {
  return (
    <>
      <PageHeader title="Reports" subtitle="Monthly attendance and audit views" />
      <ComingSoon area="Reporting and auditing" actors="HR staff, Executive management" />
    </>
  )
}
