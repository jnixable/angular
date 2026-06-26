import { createFeature, createReducer, on } from '@ngrx/store';

import { User } from '../../core/models/user.model';
import { AuthActions } from './auth.actions';

export interface AuthState {
  user: User | null;
  token: string | null;
  loading: boolean;
  error: string | null;
}

export const AUTH_TOKEN_KEY = 'swed.auth.token';

const EMPTY_STATE: AuthState = {
  user: null,
  token: null,
  loading: false,
  error: null,
};

function loadInitialState(): AuthState {
  const token = localStorage.getItem(AUTH_TOKEN_KEY);
  return token ? { ...EMPTY_STATE, token } : EMPTY_STATE;
}

export const initialAuthState: AuthState = loadInitialState();

export const authFeature = createFeature({
  name: 'auth',
  reducer: createReducer(
    initialAuthState,
    on(AuthActions.login, (state) => ({ ...state, loading: true, error: null })),
    on(AuthActions.loginSuccess, (state, { user, token }) => ({
      ...state,
      user,
      token,
      loading: false,
    })),
    on(AuthActions.loginFailure, (state, { error }) => ({ ...state, error, loading: false })),
    on(AuthActions.restoreSession, (state) => ({ ...state, error: null })),
    on(AuthActions.restoreSessionSuccess, (state, { user, token }) => ({
      ...state,
      user,
      token,
      loading: false,
    })),
    on(AuthActions.logout, () => EMPTY_STATE),
  ),
});

export const {
  name: authFeatureName,
  reducer: authReducer,
  selectUser,
  selectToken,
  selectLoading,
  selectError,
} = authFeature;
