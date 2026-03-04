declare global {
  interface Window {
    cefQuery?: (args: {
      request: string
      persistent?: boolean
      onSuccess: (resp: string) => void
      onFailure: (code: number, message: string) => void
    }) => void
  }
}

interface BridgeEnvelope<T> {
  ok: boolean
  channel: string
  data?: T
  code?: string
  message?: string
}

export class BridgeCallError extends Error {
  readonly code: string

  constructor(code: string, message: string) {
    super(message || code || 'bridge_error')
    this.name = 'BridgeCallError'
    this.code = (code || 'bridge_error').trim()
  }
}

export async function bridgeCall<T = unknown>(channel: string, payload: unknown): Promise<T> {
  if (typeof window.cefQuery !== 'function') {
    throw new BridgeCallError('bridge_unavailable', 'jcef bridge unavailable')
  }
  const request = JSON.stringify({ channel, payload })
  return new Promise<T>((resolve, reject) => {
    window.cefQuery?.({
      request,
      persistent: false,
      onSuccess: (resp) => {
        try {
          const envelope = JSON.parse(resp) as BridgeEnvelope<T>
          if (!envelope.ok) {
            reject(new BridgeCallError(
              safeText(envelope.code, 'bridge_error'),
              safeText(envelope.message, 'bridge call failed')
            ))
            return
          }
          resolve(envelope.data as T)
        } catch (error) {
          reject(error)
        }
      },
      onFailure: (code, message) => {
        reject(new BridgeCallError(`bridge_failure_${code}`, safeText(message, `bridge failure (${code})`)))
      }
    })
  })
}

export async function safeBridgeCall<T>(channel: string, payload: unknown): Promise<T | null> {
  try {
    return await bridgeCall<T>(channel, payload)
  } catch {
    return null
  }
}

export function extractBridgeError(error: unknown, fallbackCode: string): { code: string; message: string } {
  if (error instanceof BridgeCallError) {
    return {
      code: safeText(error.code, fallbackCode),
      message: safeText(error.message, 'bridge call failed')
    }
  }
  if (error instanceof Error) {
    return {
      code: fallbackCode,
      message: safeText(error.message, 'bridge call failed')
    }
  }
  return {
    code: fallbackCode,
    message: safeText(String(error ?? ''), 'bridge call failed')
  }
}

function safeText(value: string | undefined, fallback: string): string {
  const text = (value || '').trim()
  return text || fallback
}
