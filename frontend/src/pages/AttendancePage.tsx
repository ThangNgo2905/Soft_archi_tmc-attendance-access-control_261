import { ComingSoon } from '../components/common/ComingSoon'
import { PageHeader } from '../components/common/PageHeader'

export function AttendancePage() {
  return (
    <>
      <PageHeader title="Attendance" subtitle="Entry/exit events and live monitoring" />
      <ComingSoon
        area="Attendance event recording"
        actors="Production supervisors, Security guards"
      />
    </>
  )
}
