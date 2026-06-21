import { HttpErrorResponse } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { Actions, createEffect, ofType } from '@ngrx/effects';
import { catchError, map, of, switchMap, tap } from 'rxjs';

import { AuthService } from '../../core/services/auth.service';
import { AuthActions } from './auth.actions';
import { AUTH_TOKEN_KEY } from './auth.reducer';

function describeError(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    if (error.status === 401) {
      return 'Invalid User ID or password.';
    }
  }
  return 'Login failed. Please try again.';
}

export const login$ = createEffect(
  (actions$ = inject(Actions), auth = inject(AuthService)) =>
    actions$.pipe(
      ofType(AuthActions.login),
      switchMap(({ pcode, password }) =>
        auth.login(pcode, password).pipe(
          switchMap((res) =>
            auth
              .whoami(res.token)
              .pipe(map((user) => AuthActions.loginSuccess({ user, token: res.token }))),
          ),
          catchError((error) => of(AuthActions.loginFailure({ error: describeError(error) }))),
        ),
      ),
    ),
  { functional: true },
);

export const loginSuccess$ = createEffect(
  (actions$ = inject(Actions), router = inject(Router)) =>
    actions$.pipe(
      ofType(AuthActions.loginSuccess),
      tap(({ token }) => {
        localStorage.setItem(AUTH_TOKEN_KEY, token);
        void router.navigateByUrl('/home');
      }),
    ),
  { functional: true, dispatch: false },
);

export const logout$ = createEffect(
  (actions$ = inject(Actions), router = inject(Router)) =>
    actions$.pipe(
      ofType(AuthActions.logout),
      tap(() => {
        localStorage.removeItem(AUTH_TOKEN_KEY);
        void router.navigateByUrl('/login');
      }),
    ),
  { functional: true, dispatch: false },
);
