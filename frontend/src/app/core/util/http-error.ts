import { HttpErrorResponse } from '@angular/common/http';

export function describeHttpError(
  error: unknown,
  fallback = 'Something went wrong. Please try again.',
): string {
  if (error instanceof HttpErrorResponse) {
    const body = error.error;
    if (body && typeof body === 'object' && typeof (body as { error?: unknown }).error === 'string') {
      return (body as { error: string }).error;
    }
    if (typeof body === 'string' && body.trim().length > 0) {
      return body;
    }
  }
  return fallback;
}
