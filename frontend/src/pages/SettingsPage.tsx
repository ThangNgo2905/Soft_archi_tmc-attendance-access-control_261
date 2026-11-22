import { ComingSoon } from '../components/common/ComingSoon'
import { PageHeader } from '../components/common/PageHeader'

export function SettingsPage() {
  return (
    <>
      <PageHeader title="Settings" subtitle="System administration" />
      <ComingSoon area="System administration" actors="System administrator" />
    </>
  )
}
