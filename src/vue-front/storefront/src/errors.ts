import { useToast } from '@guilherme/vue-base';

function describe(error: unknown): string {
  if (typeof error === 'object' && error !== null) {
    const response = (error as { response?: { status?: number; statusText?: string } }).response;
    if (response?.status) {
      return `HTTP ${response.status} ${response.statusText ?? ''}`.trim();
    }
    const message = (error as { message?: unknown }).message;
    if (typeof message === 'string' && message) {
      return message;
    }
  }
  return 'Unknown error';
}

/** The shared error display: a sticky vue-base toast (closed by the user), with our own English summary. */
export function showError(message: string, error: unknown): void {
  useToast().add({ severity: 'error', summary: message, detail: describe(error), sticky: true });
}
