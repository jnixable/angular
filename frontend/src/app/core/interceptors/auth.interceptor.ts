import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Store } from '@ngrx/store';
import { catchError, throwError } from 'rxjs';

import { AuthActions } from '../../store/auth/auth.actions';
import { AUTH_TOKEN_KEY } from '../../store/auth/auth.reducer';

// Auth endpoints handle their own 401s (login error, session restore) and must not trigger the global logout-on-401 behaviour.
const AUTH_ENDPOINTS = ['/user/login', '/user/whoami'];

function isAuthEndpoint(url: string): boolean {
  return AUTH_ENDPOINTS.some((path) => url.includes(path));
}

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const store = inject(Store);
  const token = localStorage.getItem(AUTH_TOKEN_KEY);

  const authReq =
    token && !req.headers.has('Authorization')
      ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
      : req;

  return next(authReq).pipe(
    catchError((error: unknown) => {
      if (error instanceof HttpErrorResponse && error.status === 401 && !isAuthEndpoint(req.url)) {
        store.dispatch(AuthActions.logout());
      }
      return throwError(() => error);
    }),
  );
};
