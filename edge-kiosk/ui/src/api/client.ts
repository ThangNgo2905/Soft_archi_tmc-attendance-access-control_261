const base = import.meta.env.VITE_API_BASE_URL ?? ''

export async function request<T>(path: string, body?: unknown): Promise<T> {
  const response = await fetch(`${base}/api/v1/kiosk${path}`, body === undefined ? undefined : {
    method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body),
  })
  const result = await response.json() as { success: boolean; message: string | null; data: T }
  if (!response.ok || !result.success) throw new Error(result.message ?? `HTTP ${response.status}`)
  return result.data
}
