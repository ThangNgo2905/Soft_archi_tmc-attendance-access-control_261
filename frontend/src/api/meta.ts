import { apiGet } from './client'

export type MetaInfo = {
  name: string
  version: string
  phase: string
}

export function fetchMeta() {
  return apiGet<MetaInfo>('/api/v1/meta')
}
