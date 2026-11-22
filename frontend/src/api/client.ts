const API_BASE_URL = import.meta.env.VITE_API_BASE_URL ?? ''

export type ApiResponse<T> = {
  success: boolean
  message: string | null
  data: T
}

export async function apiGet<T>(path: string): Promise<ApiResponse<T>> {
  const response = await fetch(`${API_BASE_URL}${path}`)
  if (!response.ok) {
    throw new Error(`HTTP ${response.status}`)
  }
  return response.json() as Promise<ApiResponse<T>>
}

export async function apiPost<T>(path: string, body: unknown): Promise<ApiResponse<T>> {
  const response = await fetch(`${API_BASE_URL}${path}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  })
  if (!response.ok) {
    throw new Error(`HTTP ${response.status}`)
  }
  return response.json() as Promise<ApiResponse<T>>
}

export function getApiBaseUrl(): string {
  return API_BASE_URL || '(same origin / proxy)'
}
